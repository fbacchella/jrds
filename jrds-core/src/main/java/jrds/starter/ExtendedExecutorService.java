package jrds.starter;

import java.util.concurrent.TimeUnit;

interface ExtendedExecutorService {
    void prestartAllCoreThreads();
    void shutdown();
    void shutdownNow();
    void execute(Runnable runnable);
    int missed();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException;
}
