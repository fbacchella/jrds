package jrds.starter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.PropertiesManager;
import lombok.Getter;

public class Timer extends StarterNode {

    static final private Logger logger = LoggerFactory.getLogger(Timer.class);

    private class CollectRunnable implements Runnable {

        private final HostStarter host;

        CollectRunnable(HostStarter host) {
            this.host = host;
        }

        @Override
        public String toString() {
            return host.getRunningname();
        }

        @Override
        public void run() {
            if (Timer.this.isCollectRunning()) {
                log(Level.DEBUG, "Collect all stats for host %s",
                    host.getName());
                String collectName = Timer.this.name + "/" + "JrdsCollect-"
                                + host.getName();
                host.setRunningname(collectName);
                host.collectAll();
                host.setRunningname(collectName + ":notrunning");
            }
        }

    };

    public static final class Stats {
        @Getter
        private long duration;
        @Getter
        private Date lastCollect;

        Stats() {
            lastCollect = new Date(0);
            duration = 0;
        }

        Stats(Stats source) {
            synchronized (source) {
                lastCollect = source.lastCollect;
                duration = source.duration;
            }
        }

        public synchronized void refresh(Date lastCollect, long duration) {
            this.lastCollect = lastCollect;
            this.duration = duration;
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
                            Timer.this.log(Level.ERROR, e, "A fatal error occured during collect: %s", e);
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
        List<Runnable> toSchedule = new ArrayList<Runnable>(hostList.size());
        hostList.values().stream()
        .map(CollectRunnable::new)
        .forEach(toSchedule::add);

        if(toSchedule.size() == 0) {
            log(Level.INFO, "skipping timer, empty");
            return;
        }
        log(Level.DEBUG, "One collect is launched");
        Date start = new Date();
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
        AtomicInteger counter = new AtomicInteger(0);
        // Generate threads with a default name
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName(Timer.this.name + "/CollectorThread" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };

        tpool = new ThreadPoolExecutor(numCollectors, numCollectors, getTimeout() * 2L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(toSchedule.size()), tf) {
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
                toSchedule.stream().forEach(tpool::execute);
                tpool.shutdown();
                long collectStart = System.currentTimeMillis();
                long maxCollectTime = (getStep() - getTimeout()) * 1000L;
                while (! tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS)) {
                    if ((System.currentTimeMillis() - collectStart) > maxCollectTime && ! tpool.isTerminated()) {
                        log(Level.ERROR, "Unfinished collect, lost %d tasks", tpool.getQueue().size());
                        break;
                    } else {
                        log(Level.TRACE, "Still %s waiting or running tasks", tpool.getQueue().size());
                    }
                }
            }
        } catch (RejectedExecutionException e) {
            log(Level.DEBUG, e, "Collector thread refused new task");
        } catch (InterruptedException e) {
            log(Level.INFO, "Collect interrupted");
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log(Level.ERROR, e, "Problem while collecting data: %s", e);
        } finally {
            stopCollect();
            // Waited for late collect arrival, after the shutdownNow
            if (!tpool.isTerminated()) {
                try {
                    if (! tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS)) {
                        log(Level.ERROR, "Lost collect");
                    }
                } catch (InterruptedException e) {
                    log(Level.ERROR, "Lost collect");
                    Thread.currentThread().interrupt();
                }
            }
            collectMutex.release();
            tpool = null;
            long end = System.currentTimeMillis();
            long duration = end - start.getTime();
            stats.refresh(start, duration);
            log(Level.INFO, "Collect started at " + start + " ran for " + duration + "ms");
        }
    }

    
    @Override
    public synchronized void stopCollect() {
        super.stopCollect();
        if (tpool != null) {
            tpool.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<HostStarter> getChildsStream() {
        return hostList.values().stream();
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
        return new Stats(stats);
    }

    @Override
    public Logger getInstanceLogger() {
        return logger;
    }

}
