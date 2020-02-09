package jrds.starter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.PropertiesManager;

public class Timer extends StarterNode {

    private class CollectCallable implements Callable<Object> {

        private final HostStarter host;

        CollectCallable(HostStarter host) {
            this.host = host;
        }

        @Override
        public String toString() {
            return host.getRunningname();
        }

        @Override
        public Object call() throws Exception {
            if (host.isCollectRunning()) {
                log(Level.DEBUG, "Collect all stats for host %s", host.getName());
                String collectName = Timer.this.name + "/" + "JrdsCollect-" + host.getName();
                host.setRunningname(collectName);
                host.collectAll();
                host.setRunningname(collectName + ":notrunning");
            }
            return null;
        }

    };

    public static final class Stats implements Cloneable {
        Stats() {
            lastCollect = 0;
        }

        public long runtime = 0;
        public long lastCollect;

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#clone()
         */
        @Override
        public Object clone() throws CloneNotSupportedException {
            Stats newstates = new Stats();
            synchronized (this) {
                newstates.runtime = runtime;
                newstates.lastCollect = System.currentTimeMillis();
            }
            return newstates;
        }
    }

    public final static String DEFAULTNAME = "_default";

    private final Map<String, HostStarter> hostList = new HashMap<String, HostStarter>();
    private Semaphore collectMutex = new Semaphore(1);
    private final Stats stats = new Stats();
    private final int numCollectors;
    private final String name;
    private ThreadPoolExecutor tpool;

    public Timer(String name, PropertiesManager.TimerInfo ti) {
        super();
        this.name = name;
        setTimeout(ti.timeout);
        setStep(ti.step);
        setSlowCollectTime(ti.slowCollectTime);
        this.numCollectors = ti.numCollectors;
        registerStarter(new SocketFactory(ti.timeout));
    }

    public HostStarter getHost(HostInfo info) {
        String hostName = info.getName();
        HostStarter starter = hostList.get(hostName);
        if(starter == null) {
            starter = new HostStarter(info);
            hostList.put(hostName, starter);
            starter.setTimeout(getTimeout());
            starter.setStep(getStep());
            starter.setSlowCollectTime(getSlowCollectTime());
            starter.setParent(this);
        }
        return starter;
    }

    public Iterable<HostStarter> getAllHosts() {
        return hostList.values();
    }

    public void startTimer(java.util.Timer collectTimer) {
        TimerTask collector = new TimerTask() {
            public void run() {
                // The collect is done in a different thread
                // So a collect failure will no prevent other collect from
                // running
                Thread subcollector = new Thread("Collector/" + Timer.this.name) {
                    @Override
                    public void run() {
                        try {
                            Timer.this.collectAll();
                        } catch (RuntimeException e) {
                            Timer.this.log(Level.ERROR, e, "A fatal error occured during collect: %s", e.getMessage());
                        }
                    }
                };
                subcollector.setDaemon(true);
                subcollector.start();
            }
        };
        collectTimer.scheduleAtFixedRate(collector, getTimeout() * 1000L, getStep() * 1000L);
    }

    public void collectAll() {
        // Build the list of host that will be collected
        Set<Callable<Object>> toSchedule = new HashSet<Callable<Object>>();
        hostList.values().stream()
        .map(CollectCallable::new)
        .forEach(toSchedule::add);

        if(toSchedule.size() == 0) {
            log(Level.INFO, "skipping timer, empty");
            return;
        }
        log(Level.DEBUG, "One collect is launched");
        long start = System.currentTimeMillis();
        try {
            if(!collectMutex.tryAcquire(getTimeout(), TimeUnit.SECONDS)) {
                log(Level.ERROR, "A collect failed because a start time out");
                return;
            }
        } catch (InterruptedException e) {
            log(Level.INFO, "A collect start was interrupted");
            Thread.currentThread().interrupt();
            return;
        }
        final AtomicInteger counter = new AtomicInteger(0);
        // Generate threads with a default name
        ThreadFactory tf = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName(Timer.this.name + "/CollectorThread" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        tpool = new ThreadPoolExecutor(0, numCollectors, getTimeout(), TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(toSchedule.size()), tf) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                return new FutureTask<T>(callable) {
                    @Override
                    public String toString() {
                        return callable.toString();
                    }
                };
            }
        };
        try {
            if (startCollect()) {
                tpool.invokeAll(toSchedule, getStep() - getTimeout(), TimeUnit.SECONDS);
                stopCollect();
                // Waited for late collect arrival, after the shutdownNow
                if (!tpool.isTerminated()) {
                    tpool.shutdownNow();
                    if (! tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS)) {
                        log(Level.ERROR, "Lost collect");
                    }
                }
            }
        } catch (RejectedExecutionException ex) {
            log(Level.DEBUG, "collector thread refused");
        } catch (InterruptedException e) {
            log(Level.INFO, "Collect interrupted");
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log(Level.ERROR, e, "problem while collecting data: %s", e);
        } finally {
            tpool.shutdownNow();
            collectMutex.release();
            tpool = null;
            long end = System.currentTimeMillis();
            long duration = end - start;
            synchronized (stats) {
                stats.lastCollect = start;
                stats.runtime = duration;
            }
            log(Level.INFO, "Collect started at " + start + " ran for " + duration + "ms");
        }
    }

    public void lockCollect() throws InterruptedException {
        collectMutex.acquire();
    }

    public void releaseCollect() {
        collectMutex.release();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "timer:" + name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the stats
     */
    public Stats getStats() {
        return stats;
    }

    public void interrupt() {
        log(Level.DEBUG, "timer interrupted");
        if(tpool != null) {
            tpool.shutdownNow();
        }
    }

}
