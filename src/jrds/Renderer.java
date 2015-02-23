package jrds;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Renderer {
    private final File tmpDir;

    public class RendererRun implements Runnable {
        final Graph graph;
        volatile boolean finished = false;
        final File destFile;
        final long step;
        Date normalizedRenderTime;

        public RendererRun(Graph graph) {
            this.graph = graph;
            destFile = new File(tmpDir, Long.toHexString(graph.longHashCode()) + ".png");
            try {
                step = graph.getDataProcessor().getStep();
            } catch (IOException e) {
                throw new RuntimeException("can't get hash for graph " + graph, e);
            }
            normalizedRenderTime = Util.normalize(new Date(), step);
        }

        @Override
        protected void finalize() throws Throwable {
            clean();
            super.finalize();
        }

        public void run() {
            if( ! isReady() ) {
                writeImg();
            }
        }

        public boolean isReady() {
            //If finished, but graph end is in the future
            if( finished && graph.getEnd().getTime() > normalizedRenderTime.getTime()) {
                // If not in the same normalized time slot, graph is different, so do it again
                if (Util.normalize(new Date(), step).getTime() !=  normalizedRenderTime.getTime()) {
                    finished = false;
                }
            }
            //isReady is sometimes call before run
            if(! finished ) {
                writeImg();
            }
            if(destFile.isFile() && destFile.canRead() && destFile.length() > 0) {
                return true;
            }
            return false;
        }

        public void send(OutputStream out) throws IOException {
            if(isReady()){
                try(FileChannel inC = FileChannel.open(destFile.toPath(), StandardOpenOption.READ)) {
                    WritableByteChannel outC = Channels.newChannel(out);
                    inC.transferTo(0, destFile.length(), outC);
                }
            }
        }

        public void write() throws IOException {
            try(OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(
                    graph.getPngName())))) {
                send(out);                
            }
        }

        public void clean(){
            if(logger.isTraceEnabled()) {
                logger.trace("clean in");
                for(StackTraceElement e: Thread.currentThread().getStackTrace()) {
                    logger.trace("    " + e.toString());
                }
            }
            if( destFile.isFile() && ! destFile.delete() ) {
                logger.warn("Failed to delete " + destFile.getPath());
            }                
            finished = false;
        }

        private synchronized void writeImg() {
            try {
                if( ! finished) {
                    long starttime = System.currentTimeMillis();
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                    long middletime = System.currentTimeMillis();
                    graph.writePng(out);
                    normalizedRenderTime =  Util.normalize(new Date(), step);
                    if(logger.isTraceEnabled()) {
                        long endtime = System.currentTimeMillis();
                        long duration1 = (middletime - starttime );
                        long duration2 = (endtime - middletime );
                        logger.trace("Graph " + graph.getQualifiedName() + " renderding ran for (ms) " + duration1 + ":" + duration2);	
                    }
                }
            } catch (Exception e) {
                String graphName = null;
                String message = null;
                try {
                    graphName = graph.getQualifiedName();
                    message = "Error rendering graph %s: %s";
                } catch (Exception e1) {
                    //throws if the graph is not totally configured
                    graphName = graph.getNode().getProbe().getName() + "/" + graph.getNode().getGraphDesc().getGraphName();
                    message = "Error rendering incomplete graph %s: %s";
                }
                Util.log(null, Logger.getLogger(getClass()), Level.ERROR, e, message, graphName, e.getMessage());
                Throwable cause = e.getCause();
                if(cause != null)
                    logger.error("    Cause was: " + cause);
            } finally {						
                //Always set to true, we do not try again in case of failure
                finished = true;
            }
        }

        @Override
        public String toString() {
            return graph.toString();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return graph.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Graph other = ((RendererRun) obj).graph;
            return graph.equals(other);
        }

    };

    static private final Logger logger = Logger.getLogger(Renderer.class);
    static private final float hashTableLoadFactor = 0.75f;
    final private AtomicInteger counter = new AtomicInteger(0);

    private final ExecutorService tpool;
    private final Map<Long, RendererRun> rendered;

    public Renderer(final int cacheSize, int renderThreads, File tmpDir) {
        this.tmpDir = tmpDir;

        //Define the threads pool for graph rendering
        tpool =  Executors.newFixedThreadPool(renderThreads, 
                new ThreadFactory() {
            public Thread newThread(Runnable r) {
                String threadName = "RendererThread" + counter.getAndIncrement();
                Thread t = new Thread(r, threadName);
                t.setDaemon(true);
                logger.debug(Util.delayedFormatString("New renderer thread: %s", threadName));
                return t;
            }
        }
                );

        Map<Long, RendererRun> m = new LinkedHashMap<Long, RendererRun>(cacheSize + 5 , hashTableLoadFactor, false) {

            /* (non-Javadoc)
             * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
             */
            @Override
            protected boolean removeEldestEntry(Entry<Long, RendererRun> eldest) {
                if (size() < cacheSize) {
                    return false;
                }
                RendererRun rr = eldest.getValue();
                if( rr != null && rr.finished) {
                    return true;
                }
                else if (rr != null &&  size() > cacheSize){
                    Util.log(null, logger, Level.DEBUG, null, "Graph queue too short, it's now %d instead of %d", size(), cacheSize);
                }
                return false;
            }

            /* (non-Javadoc)
             * @see java.util.HashMap#remove(java.lang.Object)
             */
            @Override
            public RendererRun remove(Object key) {
                RendererRun rr =  super.remove(key);
                rr.clean();
                return rr;
            }

            /* (non-Javadoc)
             * @see java.lang.Object#finalize()
             */
            @Override
            protected void finalize() throws Throwable {
                for(RendererRun rr: values()) {
                    rr.clean();
                }
                super.finalize();
            }

        };
        rendered = Collections.synchronizedMap(m);	
    }

    public RendererRun render(Graph graph) {
        RendererRun runRender = rendered.get(graph.longHashCode());
        if(runRender == null) {
            synchronized(rendered){
                if( ! rendered.containsKey(graph) ) {
                    runRender = new RendererRun(graph);
                    rendered.put(graph.longHashCode(), runRender);
                } else {
                    runRender = rendered.get(graph.longHashCode());
                }
            }            
        }
        if( ! runRender.isReady() && ! tpool.isShutdown()) {
            try {
                tpool.execute(runRender);
            }
            catch(RejectedExecutionException ex) {
                logger.warn("Render thread dropped for graph " + graph);
            }
            logger.debug(Util.delayedFormatString("wants to render %s", runRender));
        } 
        return runRender;
    }

    public Graph getGraph(long key) {
        Graph g = null;
        if(key != 0) {
            RendererRun rr = rendered.get(key);
            if(rr != null)
                g = rr.graph;
        }
        return g;
    }

    public boolean isReady(Graph graph) {
        return render(graph).isReady();
    }

    public void send(Graph graph, OutputStream out) throws IOException {
        RendererRun runRender = render(graph);
        if(runRender != null && runRender.isReady()) {
            runRender.send(out);
        }
        else {
            Util.log(graph, logger, Level.INFO, null, "No valid precalculated render found for %s", graph);
            //No precalculation found, so we do it right now
            graph.writePng(out);
        }
    }

    public FileChannel sendInfo(Graph graph) {
        RendererRun runRender = render(graph);
        if(runRender != null && runRender.isReady()) {
            try {
                return new FileInputStream(runRender.destFile).getChannel();
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        else {
            return null;
        }	    
    }

    public Collection<RendererRun> getWaitings() {
        return new HashSet<RendererRun>(rendered.values());
    }

    public void finish() {
        tpool.shutdownNow();
        for(RendererRun rr: rendered.values()) {
            rr.clean();
        }
    }
}
