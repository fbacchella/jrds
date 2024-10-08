package jrds.starter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
                String oldThreadName = Thread.currentThread().getName();
                log(Level.DEBUG, "Collecting all stats for host %s", host.getName());
                host.setRunningname(oldThreadName + "/" + host.getName());
                host.collectAll();
                host.setRunningname(oldThreadName);
            }
        }

    }

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

    private final Map<String, HostStarter> hostList = new HashMap<>();
    private final Semaphore collectMutex = new Semaphore(1);
    private final Stats stats = new Stats();
    private final int numCollectors;
    private final String name;
    private final AtomicInteger collectCount = new AtomicInteger(0);
    private volatile Thread collectThread = null;

    public Timer(String name, PropertiesManager.TimerInfo ti) {
        super();
        this.name = name;
        setTimeout(ti.timeout);
        setStep(ti.step);
        setSlowCollectTime(ti.slowCollectTime);
        this.numCollectors = ti.numCollectors;
        registerStarter(SocketFactory.class, SocketFactory.class.getName(), () -> new SocketFactory(ti.timeout));
    }

    public HostStarter getHost(HostInfo info) {
        return hostList.computeIfAbsent(info.getName(), k -> {
            HostStarter s = new HostStarter(info);
            s.setTimeout(getTimeout());
            s.setStep(getStep());
            s.setSlowCollectTime(getSlowCollectTime());
            s.setParent(this);
            return s;
        });
    }

    public Iterable<HostStarter> getAllHosts() {
        return hostList.values();
    }

    public void startTimer(java.util.Timer collectTimer) {
        MDC.put("timer", name);
        TimerTask collector = new TimerTask() {
            public void run() {
                MDC.put("timer", name);
                collectCount.incrementAndGet();
                // The collect is done in a different thread
                // So a collect failure will no prevent other collect from running
                Thread subcollector = new Thread(Timer.this::collectAll, "Collect/" + Timer.this.name);
                subcollector.setDaemon(true);
                subcollector.setUncaughtExceptionHandler((t, ex) -> Timer.this.log(Level.ERROR, ex, "A fatal error occured during collect: %s", ex));
                subcollector.start();
                MDC.remove("timer");
            }
        };
        collectTimer.scheduleAtFixedRate(collector, getTimeout() * 1000L, getStep() * 1000L);
        MDC.remove("timer");
    }

    public void collectAll() {
        MDC.put("collectIteration", String.valueOf(collectCount.get()));
        MDC.put("timer", name);
        // Build the list of host that will be collected
        List<Runnable> toSchedule = new ArrayList<>(hostList.size());
        hostList.values().stream()
                .map(CollectRunnable::new)
                .forEach(toSchedule::add);

        if (toSchedule.size() == 0) {
            log(Level.INFO, "skipping timer, empty");
            return;
        }
        log(Level.DEBUG, "One collect is launched");
        Date start = new Date();
        try {
            if (!collectMutex.tryAcquire(getTimeout(), TimeUnit.SECONDS)) {
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
            Thread t = new Thread(r) {
                @Override
                public void run() {
                    MDC.put("collectIteration", String.valueOf(collectCount.get()));
                    MDC.put("collectorInstance", String.valueOf(counter.incrementAndGet()));
                    MDC.put("timer", name);
                    super.run();
                    MDC.remove("collectIteration");
                    MDC.remove("collectorInstance");
                    MDC.remove("timer");
                }
            };
            t.setName("Collect/" + Timer.this.name + "/Collector");
            t.setDaemon(true);
            return t;
        };

        long maxCollectTime = (getStep() - getTimeout());
        ThreadPoolExecutor tpool = new ThreadPoolExecutor(numCollectors, numCollectors, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(toSchedule.size()), tf);
        tpool.allowCoreThreadTimeOut(true);
        try {
            if (startCollect()) {
                tpool.prestartAllCoreThreads();
                toSchedule.stream().forEach(tpool::execute);
                tpool.shutdown();
                collectThread = Thread.currentThread();
                boolean terminated = tpool.awaitTermination(maxCollectTime, TimeUnit.SECONDS);
                if (!terminated) {
                    log(Level.ERROR, "Unfinished collect, lost %d tasks", tpool.getQueue().size());
                }
            }
        } catch (RejectedExecutionException e) {
            log(Level.DEBUG, e, "Collector thread refused new task");
        } catch (InterruptedException e) {
            log(Level.INFO, "Collect interrupted, lost %d tasks", tpool.getQueue().size());
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log(Level.ERROR, e, "Problem while collecting data: %s", e);
        } finally {
            collectThread = null;
            int missed = tpool.getQueue().size();
            stopCollect();
            Thread.yield();
            // Waited for late collect arrival, after the stopCollect
            if (!tpool.isTerminated()) {
                tpool.shutdownNow();
                String missedMessage = missed == 0 ? "" : ", missed " + missed + " hosts";
                try {
                    if (! tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS)) {
                        log(Level.ERROR, "Lost collect" + missedMessage);
                    }
                } catch (InterruptedException e) {
                    log(Level.ERROR, "Lost collect" + missedMessage);
                    Thread.currentThread().interrupt();
                }
            }
            collectMutex.release();
            long end = System.currentTimeMillis();
            long duration = end - start.getTime();
            stats.refresh(start, duration);
            log(Level.INFO, "Collect started at " + start + " ran for " + duration + "ms");
            MDC.remove("timer");
        }
    }

    
    @Override
    public synchronized void stopCollect() {
        Optional.ofNullable(collectThread).ifPresent(Thread::interrupt);
        super.stopCollect();
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
