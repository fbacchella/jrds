package jrds.starter;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;

import org.apache.log4j.Level;

public class Timer extends StarterNode {

    public static final class Stats {
        Stats() {
            lastCollect = new Date(0);
        }
        public long runtime = 0;
        public Date lastCollect;
    }

    private final Set<HostStarter> hostList = new HashSet<HostStarter>();
    private Semaphore collectMutex = new Semaphore(1);
    private final Stats stats = new Stats();
    private int timeout;
    private int numCollectors;
    private int step;
    private TimerTask collector;

    /* (non-Javadoc)
     * @see jrds.starter.StarterNode#configureStarters(jrds.PropertiesManager)
     */
    @Override
    public void configureStarters(PropertiesManager pm) {
        numCollectors = pm.collectorThreads;
        super.configureStarters(pm);
    }

    public void addProbe(Probe<?,?> probe) {
        RdsHost host = probe.getHost();
        if(! hostList.contains(arg0)(host))
            hostList.add(host);
        hostList.get(host).add(probe);
    }
    
    public void startTimer(java.util.Timer collectTimer) {
        collector = new TimerTask () {
            public void run() {
                try {
                    Timer.this.collectAll();
                } catch (RuntimeException e) {
                    Timer.this.log(Level.FATAL,"A fatal error occured during collect: ", e);
                }
            }
        };
        collectTimer.schedule(collector, 5000L, Timer.this.step * 1000L);
    }
    
    public void collectAll() {
        log(Level.DEBUG, "One collect is launched");
        Date start = new Date();
        try {
            if( ! collectMutex.tryAcquire(timeout, TimeUnit.SECONDS)) {
                log(Level.FATAL, "A collect failed because a start time out");
                return;
            }
        } catch (InterruptedException e) {
            log(Level.FATAL, "A collect start was interrupted");
            return;
        }
        try {
            final Object counter = new Object() {
                int i = 0;
                @Override
                public String toString() {
                    return Integer.toString(i++);
                }

            };
            ExecutorService tpool =  Executors.newFixedThreadPool(numCollectors, 
                    new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CollectorThread" + counter);
                    t.setDaemon(true);
                    log(Level.DEBUG, "New thread name:" + t.getName());
                    return t;
                }
            }
                    );
            startCollect();
            for(Map.Entry<RdsHost, Set<Probe<?,?>>> entry: hostList.entrySet()) {
                final RdsHost host = entry.getKey();
                final Set<Probe<?,?>> probes = entry.getValue();
                if( ! isCollectRunning())
                    break;
                log(Level.DEBUG, "Collect all stats for host " + host.getName());
                Runnable runCollect = new Runnable() {
                    public void run() {
                        Thread.currentThread().setName("JrdsCollect-" + host.getName());
                        Timer.this.collectProbes(host, probes);
                        Thread.currentThread().setName("JrdsCollect-" + host.getName() + ":finished");
                    }
                    @Override
                    public String toString() {
                        return Thread.currentThread().toString();
                    }
                };
                try {
                    tpool.execute(runCollect);
                }
                catch(RejectedExecutionException ex) {
                    log(Level.DEBUG, "collector thread dropped for host " + oneHost.getName());
                }
            }
            tpool.shutdown();
            try {
                tpool.awaitTermination(step - timeout * 2 , TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log(Level.WARN, "Collect interrupted");
            }
            stopCollect();
            if( ! tpool.isTerminated()) {
                //Second chance, we wait for the time out
                boolean emergencystop = false;
                try {
                    emergencystop = tpool.awaitTermination(timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log(Level.WARN, "Collect interrupted in last chance");
                }
                if(! emergencystop) {
                    //logger.info("Some probes are hanged");

                    //                  if(! emergencystop) {
                    log(Level.WARN, "Some task still alive, needs to be killed");
                    //                      //Last chance to commit results
                    List<Runnable> timedOut = tpool.shutdownNow();
                    if(! timedOut.isEmpty()) {
                        log(Level.WARN, "Still " + timedOut.size() + " waiting probes: ");
                        for(Runnable r: timedOut) {
                            log(Level.WARN, r.toString());
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            log(Level.ERROR, "problem while collecting data: ", e);
        }
        finally {
            //StoreOpener.
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
    
    private void collectProbes(RdsHost host, Set<Probe<?,?>> probes) {
        log(Level.DEBUG, "Starting collect for %s", this);
        long start = System.currentTimeMillis();
        startCollect();
        for(Probe<?,?> currrd: probes) {
            if(! isCollectRunning() )
                break;
            long duration = (System.currentTimeMillis() - start) / 1000 ;
            if(duration > (currrd.getStep() / 2 )) {
                log(Level.ERROR, "Collect too slow: %ds", duration);
                break;
            }
            currrd.collect();
        }
        stopCollect();
        long end = System.currentTimeMillis();
        float elapsed = (end - start)/1000f;
        log(Level.DEBUG, "Collect time for %s: %fs", host.getName(), elapsed);
        
    }

}
