package jrds.starter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import lombok.Builder;

@Builder
class TimerThreadService {

    public class RealExtendedExecutorService implements ExtendedExecutorService {
        private final ThreadPoolExecutor tpool;

        public RealExtendedExecutorService(ThreadPoolExecutor tpool) {
            this.tpool = tpool;
        }

        @Override
        public void prestartAllCoreThreads() {
            tpool.prestartAllCoreThreads();
        }

       @Override
        public void shutdown() {
            tpool.shutdown();
        }

        @Override
        public void shutdownNow() {
            tpool.shutdownNow();
        }

        @Override
        public void execute(Runnable runnable) {
            tpool.execute(runnable);
        }

        @Override
        public int missed() {
            return tpool.getQueue().size();
        }

        @Override
        public boolean isTerminated() {
            return tpool.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return tpool.awaitTermination(timeout, TimeUnit.SECONDS);
        }
    }
    private final String timerName;
    private final int numCollectors;
    private final BiConsumer<AtomicInteger, Runnable> wrapRunner;

    ExtendedExecutorService getExecutor(int toSchedule, AtomicInteger counter) {
        // Generate threads with a default name
        ThreadFactory tf = r -> {
            Thread t = new Thread(() -> wrapRunner.accept(counter, r));
            t.setName("Collect/" + timerName + "/Collector");
            t.setDaemon(true);
            return t;
        };
        ThreadPoolExecutor tpool = new ThreadPoolExecutor(numCollectors, numCollectors, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(toSchedule), tf);
        tpool.allowCoreThreadTimeOut(true);
        return new RealExtendedExecutorService(tpool);
    }

    void printRelease() {
        System.err.println(17);
    }
}
