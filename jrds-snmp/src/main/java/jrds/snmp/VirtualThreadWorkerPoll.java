package jrds.snmp;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.snmp4j.util.WorkerPool;
import org.snmp4j.util.WorkerTask;

class VirtualThreadWorkerPoll implements WorkerPool {
    private static final AtomicLong POOL_COUNT = new AtomicLong(0);

    Map<Thread, WorkerTask> threads = new ConcurrentHashMap();
    private final AtomicLong threadCount = new AtomicLong(0);
    private final long poolIdentifier = POOL_COUNT.incrementAndGet();

    @Override
    public void execute(WorkerTask task) {
        Thread.ofVirtual()
                .name(String.format("Snmp4jVirtualThreadPoll-%s-%d", poolIdentifier, threadCount.incrementAndGet()))
                .start(() -> runTask(task));
    }

    private void runTask(WorkerTask task) {
        threads.put(Thread.currentThread(), task);
        try {
            task.run();
        } finally {
            threads.remove(Thread.currentThread());
        }
    }

    @Override
    public boolean tryToExecute(WorkerTask task) {
        execute(task);
        return true;
    }

    @Override
    public void stop() {
        threads.values().forEach(WorkerTask::terminate);
        boolean wasInterrupted = false;
        for (Thread t : threads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                wasInterrupted = true;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cancel() {
        threads.values().forEach(WorkerTask::terminate);
        threads.values().forEach(WorkerTask::interrupt);
    }

    @Override
    public boolean isIdle() {
        return true;
    }

}
