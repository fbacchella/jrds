package jrds.starter;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jrds.HostInfo;
import jrds.PropertiesManager;

import org.apache.log4j.Level;

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
            log(Level.DEBUG, "Collect all stats for host %s", host.getName());
            String collectName = Timer.this.name  + "/" + "JrdsCollect-" + host.getName();
            host.setRunningname(collectName);
            host.collectAll();
            host.setRunningname(collectName + ":notrunning");
            return null;
        }

    };

    public static final class Stats implements Cloneable {
        Stats() {
            lastCollect = new Date(0);
        }
        public long runtime = 0;
        public Date lastCollect;
        /* (non-Javadoc)
         * @see java.lang.Object#clone()
         */
        @Override
        public Object clone() throws CloneNotSupportedException {
            Stats newstates = new Stats();
            synchronized(this) {
                newstates.runtime = runtime;
                newstates.lastCollect = new Date(lastCollect.getTime());
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
    private final Queue<Future<Object>> running = new ConcurrentLinkedQueue<>();
    private ThreadPoolExecutor tpool;

    public Timer(String name, PropertiesManager.TimerInfo ti) {
        super();
        this.name = name;
        setTimeout(ti.timeout);
        setStep(ti.step);
        setSlowCollectTime(ti.slowCollectTime);
        this.numCollectors = ti.numCollectors;
    }

    public HostStarter getHost(HostInfo info) {
        String hostName = info.getName();
        HostStarter starter = hostList.get(hostName);
        if(starter == null) {
            starter = new HostStarter(info);
            hostList.put(hostName, starter);
            starter.setTimeout(getTimeout());
            starter.setStep(getStep());
            starter.setParent(this);
        }
        return starter;
    }

    public Iterable<HostStarter> getAllHosts() {
        return hostList.values();
    }

    public void startTimer(java.util.Timer collectTimer) {
        TimerTask collector = new TimerTask () {
            public void run() {
                // The collect is done in a different thread
                // So a collect failure will no prevent other collect from running
                Thread subcollector = new Thread("Collector/" + Timer.this.name) {
                    @Override
                    public void run() {
                        try {
                            Timer.this.collectAll();
                        } catch (RuntimeException e) {
                            Timer.this.log(Level.FATAL, e, "A fatal error occured during collect: %s", e.getMessage());
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
        for(final HostStarter host: hostList.values()) {
            Callable<Object> runCollect = new CollectCallable(host);
            toSchedule.add(runCollect);
        }
        if(toSchedule.size() == 0) {
            log(Level.INFO, "skipping timer, empty");
            return;
        }
        log(Level.DEBUG, "One collect is launched");
        Date start = new Date();
        try {
            if( ! collectMutex.tryAcquire(getTimeout(), TimeUnit.SECONDS)) {
                log(Level.FATAL, "A collect failed because a start time out");
                return;
            }
        } catch (InterruptedException e) {
            log(Level.FATAL, "A collect start was interrupted");
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
        synchronized (running) {
            // Generate a ThreadPoolExecutor where Runnable.toString return
            // Callable.toString
            tpool = new ThreadPoolExecutor(numCollectors, numCollectors,
                    0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(toSchedule.size()),
                    tf) {

                @Override
                protected <T> RunnableFuture<T> newTaskFor(
                        final Callable<T> callable) {
                    return new FutureTask<T>(callable){
                        @Override
                        public String toString() {
                            return callable.toString();
                        }
                    };
                }
            };
        }
        running.clear();
        startCollect();
        try {
            try {
                if(isCollectRunning()) {
                    List<Future<Object>> scheduled = tpool.invokeAll(toSchedule, getStep() - getTimeout() * 2 , TimeUnit.SECONDS);
                    running.addAll(scheduled);
                    tpool.shutdown();
                    tpool.awaitTermination(getStep() - getTimeout() * 2 , TimeUnit.SECONDS);
                }
            } catch(RejectedExecutionException ex) {
                log(Level.DEBUG, "collector thread refused");
            } catch (InterruptedException e) {
                log(Level.INFO, "Collect interrupted");
            }
            stopCollect();
            if( ! tpool.isTerminated()) {
                //Second chance, we wait for the time out
                boolean emergencystop = false;
                try {
                    emergencystop = ! tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log(Level.INFO, "Collect interrupted in last chance");
                }
                if(emergencystop) {
                    log(Level.INFO, "Some task still alive, needs to be killed");
                    //Last chance to commit results
                    tpool.shutdownNow();
                    dumpCollectHanged();
                }
            }
        } catch (RuntimeException e) {
            log(Level.ERROR, e, "problem while collecting data: %s", e);
        }
        finally {
            synchronized (running) {
                tpool.shutdown();
                tpool = null;
            }
            collectMutex.release();             
        }
        Date end = new Date();
        long duration = end.getTime() - start.getTime();
        synchronized(stats) {
            stats.lastCollect = start;
            stats.runtime = duration;
        }
        System.gc();
        log(Level.INFO, "Collect started at "  + start + " ran for " + duration + "ms");
    }

    public void lockCollect() throws InterruptedException {
        collectMutex.acquire();
    }

    public void releaseCollect() {
        collectMutex.release();
    }

    /* (non-Javadoc)
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
        synchronized (running) {
            if (tpool != null) {
                tpool.shutdownNow();
            }
        }
        dumpCollectHanged();
    }

    private void dumpCollectHanged() {
        while(! running.isEmpty()) {
            try {
                Future<Object> waiting = running.iterator().next();
                if(waiting.isDone() || waiting.isCancelled()) {
                    running.remove(waiting);
                } else {
                    waiting.cancel(true);
                    log(Level.INFO, "%s blocked", waiting.toString());
                    Thread.sleep(10);
                }
            } catch (NoSuchElementException | InterruptedException e) {
            }
        }

    }

}
