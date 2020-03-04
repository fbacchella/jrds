package jrds;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class Renderer {

    public class RendererRun implements Runnable {
        private final Graph graph;
        volatile boolean finished = false;
        private final ReentrantLock running = new ReentrantLock();
        private final File destFile;

        public RendererRun(Graph graph) throws IOException {
            this.graph = graph;
            destFile = new File(tmpDir, Integer.toHexString(graph.hashCode()) + ".png");
        }

        public void run() {
            try {
                writeImg();
            } catch (Exception e) {
                Util.log(this, logger, Level.ERROR, e, "Uncatched error while rendering graph %s: %s", graph, e);
            }
        }

        public boolean isReady() {
            // isReady is sometimes call before run
            writeImg();
            return destFile.isFile() && destFile.canRead() && destFile.length() > 0;
        }

        public void send(OutputStream out) throws IOException {
            if (isReady()) {
                WritableByteChannel outC = Channels.newChannel(out);
                try (FileInputStream inStream = new FileInputStream(destFile)) {
                    FileChannel inC = inStream.getChannel();
                    inC.transferTo(0, destFile.length(), outC);
                }
            }
        }

        public void write() throws IOException {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(graph.getPngName())))) {
                send(out);
            }
        }

        public void clean() {
            if (!destFile.isFile() || !destFile.delete()) {
                logger.warn("Failed to delete {}", destFile.getPath());
            }
        }

        private void writeImg() {
            running.lock();
            try {
                if (!finished) {
                    long starttime = System.currentTimeMillis();
                    try(OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile))) {
                        graph.writePng(out);
                    };
                    long middletime = System.currentTimeMillis();
                    if(logger.isTraceEnabled()) {
                        long endtime = System.currentTimeMillis();
                        long duration1 = (middletime - starttime);
                        long duration2 = (endtime - middletime);
                        logger.trace("Graph " + graph.getQualifiedName() + " renderding ran for (ms) " + duration1 + ":" + duration2);
                    }
                }
            } catch (IOException e) {
                Util.log(this, logger, Level.ERROR, e, "Error with temporary output file: %s", e);
            } catch (Exception e) {
                String message;
                try {
                    String graphName = graph.getQualifiedName();
                    message = String.format("Error rendering graph %s: %s", graphName, Util.resolveThrowableException(e));
                } catch (Exception e1) {
                    String graphName = graph.getNode().getProbe().getName() + "/" + graph.getNode().getGraphDesc().getGraphName();
                    message = String.format("Error rendering incomplete graph %s: %s", graphName, Util.resolveThrowableException(e));
                }
                Util.log(this, logger, Level.ERROR, e, message);
            } finally {
                // Always set to true, we do not try again in case of failure
                finished = true;
                running.unlock();
                clean();
            }
        }

        @Override
        public String toString() {
            return graph.toString();
        }

    }

    static private final Logger logger = LoggerFactory.getLogger(Renderer.class);
    static private final float hashTableLoadFactor = 0.75f;
    static private final AtomicInteger counter = new AtomicInteger(0);

    private final ExecutorService tpool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            String threadName = "RendererThread" + counter.getAndIncrement();
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            logger.debug("New thread name: {}", threadName);
            return t;
        }
    });
    private int cacheSize;
    private final Map<Integer, RendererRun> rendered;
    private final File tmpDir;

    public Renderer(int cacheSize, File tmpDir) {
        this.tmpDir = tmpDir;
        this.cacheSize = cacheSize;
        Map<Integer, RendererRun> m = new LinkedHashMap<Integer, RendererRun>(cacheSize + 5, hashTableLoadFactor, true) {
            /*
             * (non-Javadoc)
             * 
             * @see
             * java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
             */
            @Override
            protected boolean removeEldestEntry(Entry<Integer, RendererRun> eldest) {
                RendererRun rr = eldest.getValue();
                if(rr != null && rr.finished && size() > Renderer.this.cacheSize) {
                    return true;
                } else if(rr != null && size() > Renderer.this.cacheSize) {
                    Util.log(null, logger, Level.DEBUG, null, "Graph queue too short, it's now %d instead of %d", size(), Renderer.this.cacheSize);
                }
                return false;
            }

            /*
             * (non-Javadoc)
             * 
             * @see java.util.HashMap#remove(java.lang.Object)
             */
            @Override
            public RendererRun remove(Object key) {
                RendererRun rr = super.remove(key);
                rr.clean();
                return rr;
            }
        };
        rendered = Collections.synchronizedMap(m);
    }

    public RendererRun render(Graph graph) {
        return rendered.computeIfAbsent(graph.hashCode(), i -> {
            RendererRun runRender = null;
            try {
                runRender = new RendererRun(graph);
                tpool.execute(runRender);
                logger.debug("wants to render {}", runRender);
                return runRender;
            } catch (RejectedExecutionException ex) {
                runRender.clean();
                logger.warn("Render thread dropped for graph {}", graph);
                return null;
            } catch (IOException ex) {
                Util.log(this, logger, Level.WARN, ex, "Fail to render %s: %s", ex);
                return null;
            }
        });
    }

    public Graph getGraph(int key) {
        if(key != 0) {
            return Optional.ofNullable(rendered.get(key)).map(rr -> rr.graph).orElse(null);
        } else {
            return null;
        }
    }

    public boolean isReady(Graph graph) {
        return Optional.ofNullable(render(graph)).map(RendererRun::isReady).orElse(false);
    }

    public void send(Graph graph, OutputStream out) throws IOException {
        RendererRun runRender = null;
        try {
            runRender = rendered.get(graph.hashCode());
        } catch (Exception e) {
            logger.error("Error with probe: {}", Util.resolveThrowableException(e));
        }
        if(runRender != null && runRender.isReady()) {
            runRender.send(out);
        } else {
            logger.info("No valid precalculated render found for {}", graph);
            // No precalculation found, so we do it right now
            graph.writePng(out);
        }
    }

    public FileChannel sendInfo(Graph graph) {
        RendererRun runRender = null;
        try {
            runRender = rendered.get(graph.hashCode());
        } catch (Exception e) {
            logger.error("Error with probe: " + e);
        }
        if(runRender != null && runRender.isReady()) {
            try {
                return FileChannel.open(runRender.destFile.toPath(), StandardOpenOption.READ);
            } catch (IOException e) {
                logger.error("Can't read graph cache file: " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public Collection<RendererRun> getWaitings() {
        return rendered.values();
    }

    public void finish() {
        tpool.shutdownNow();
        for(RendererRun rr: rendered.values()) {
            rr.clean();
        }
    }
}
