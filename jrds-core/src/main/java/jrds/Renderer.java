package jrds;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class Renderer {

    public class RendererRun implements Runnable {
        private final Graph graph;
        volatile boolean finished;
        private final ReentrantLock running = new ReentrantLock();
        private final File destFile;

        public RendererRun(Graph graph) {
            this.graph = graph;
            destFile = new File(tmpDir, Integer.toHexString(graph.hashCode()) + ".png");
            finished = destFile.isFile() && destFile.canRead() && destFile.length() > 0;
        }

        public void run() {
            try {
                writeImg();
            } catch (Exception e) {
                Util.log(this, logger, Level.ERROR, e, "Uncatched error while rendering graph %s: %s", graph, e);
                clean();
            }
        }

        public boolean isReady() {
            // isReady is sometimes call before run
            writeImg();
            return finished;
        }

        public void send(OutputStream out) throws IOException {
            if (isReady()) {
                WritableByteChannel outC = Channels.newChannel(out);
                send(outC);
            }
        }

        public void send(WritableByteChannel out) throws IOException {
            if (isReady()) {
                try (FileChannel inC = FileChannel.open(destFile.toPath(), StandardOpenOption.READ)) {
                    inC.transferTo(0, destFile.length(), out);
                }
            }
        }

        public void write() throws IOException {
            try (FileChannel out = FileChannel.open(Paths.get(graph.getPngName()), 
                                                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                send(out);
            }
        }

        public void clean() {
            try {
                running.lockInterruptibly();
                if (tmpDir.exists() && destFile.exists() && destFile.isFile()) {
                    Files.delete(destFile.toPath());
                }
            } catch (IOException ex) {
                logger.warn("Failed to delete {}: {}", destFile.getPath(), ex);
            } catch (InterruptedException e) {
                // Locked failed, will not do anything
                Thread.currentThread().interrupt();
            } finally {
                // Always set to true, we do not try again in case of failure
                finished = false;
                if (running.isHeldByCurrentThread()) {
                    running.unlock();
                }
            }
        }

        private void writeImg() {
            try {
                running.lockInterruptibly();
                if (!finished) {
                    long starttime = System.currentTimeMillis();
                    try (WritableByteChannel out = FileChannel.open(destFile.toPath(),
                                                                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        graph.writePng(out);
                    }
                    long middletime = System.currentTimeMillis();
                    if(logger.isTraceEnabled()) {
                        long endtime = System.currentTimeMillis();
                        long duration1 = (middletime - starttime);
                        long duration2 = (endtime - middletime);
                        logger.trace("Graph " + graph.getQualifiedName() + " renderding ran for (ms) " + duration1 + ":" + duration2);
                    }
                }
            } catch (InterruptedException e) {
                // Locked failed, will not do anything
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                Util.log(this, logger, Level.ERROR, e, "Error writting temporary output file: %s", e);
                clean();
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
                clean();
            } finally {
                // Always set to true, we do not try again in case of failure
                finished = true;
                if (running.isHeldByCurrentThread()) {
                    running.unlock();
                }
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

    private final ExecutorService tpool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3, r -> {
        String threadName = "RendererThread" + counter.getAndIncrement();
        Thread t = new Thread(r, threadName);
        t.setDaemon(true);
        logger.debug("New thread name: {}", threadName);
        return t;
    });

    private int cacheSize;
    private final Map<Integer, RendererRun> rendered;
    private final File tmpDir;

    public Renderer(int cacheSize, File tmpDir) {
        this.tmpDir = tmpDir;
        this.cacheSize = cacheSize;
        Map<Integer, RendererRun> m = new LinkedHashMap<>(cacheSize + 5, hashTableLoadFactor, true) {
            /*
             * (non-Javadoc)
             *
             * @see
             * java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
             */
            @Override
            protected boolean removeEldestEntry(Entry<Integer, RendererRun> eldest) {
                RendererRun rr = eldest.getValue();
                if (rr != null && rr.finished && size() > Renderer.this.cacheSize) {
                    rr.clean();
                    return true;
                } else if (rr != null && size() > Renderer.this.cacheSize) {
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
            }
        });
    }

    public Graph getGraph(int key) {
        return Optional.ofNullable(key)
                        .filter(k -> key != 0)
                        .map(rendered::get)
                        .map(rr -> rr.graph).orElse(null);
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
        } catch (Exception ex) {
            logger.error("Error with probe: {}", Util.resolveThrowableException(ex));
        }
        if (runRender != null && runRender.isReady()) {
            try {
                return FileChannel.open(runRender.destFile.toPath(), StandardOpenOption.READ);
            } catch (IOException e) {
                Util.log(this, logger, Level.ERROR, e, "Can't read graph cache file: %s", e);
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
