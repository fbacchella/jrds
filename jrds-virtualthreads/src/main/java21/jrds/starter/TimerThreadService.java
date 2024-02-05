package jrds.starter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import lombok.Builder;

@Builder
class TimerThreadService {

    public class RealExtendedExecutorService implements ExtendedExecutorService {
        private final ExecutorService tpool;

        public RealExtendedExecutorService(ExecutorService tpool) {
            this.tpool = tpool;
        }

        @Override
        public void prestartAllCoreThreads() {
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
            return 0;
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
        Thread.Builder builder = Thread.ofVirtual();
        // Generate threads with a default name
        ThreadFactory tf = r -> {
            Thread t = builder.name("Collect/" + timerName + "/Collector").unstarted(() -> wrapRunner.accept(counter, r));
            t.setDaemon(true);
            return t;
        };
        ExecutorService tpool = Executors.newThreadPerTaskExecutor(tf);
        return new RealExtendedExecutorService(tpool);
    }

    void printRelease() {
        System.err.println(21);
    }
}
