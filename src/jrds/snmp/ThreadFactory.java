package jrds.snmp;
/*_############################################################################
_## 
_##  Extension of DefaultThreadFactory.java  
_## 
_##  Copyright (C) 2003-2013  Frank Fock and Jochen Katz (SNMP4J.org)
_##  Copyright (C) 2013  Fabrice Bacchella (jrds.fr)
_##
_##########################################################################*/

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.io.Writer;

import jrds.Util;

import org.apache.log4j.Logger;
import org.snmp4j.util.*;
import org.snmp4j.SNMP4JSettings;

/**
 * The <code>DefaultThreadFactory</code> creates {@link WorkerTask} instances
 * that allow concurrent execution of tasks. By default it uses a timeout
 * of 60 seconds (1 min.) when joining threads on behalf of an call of the
 * {@link WorkerTask#join()} method. By setting
 *
 * @author Frank Fock
 * @version 1.10.2
 * @since 1.8
 */
public class ThreadFactory implements org.snmp4j.util.ThreadFactory {

    static private final Logger logger = Logger.getLogger(ThreadFactory.class);

    private long joinTimeout;

    public ThreadFactory() {
        joinTimeout = SNMP4JSettings.getThreadJoinTimeout();
    }

    /**
     * Creates a new thread of execution for the supplied task.
     *
     * @param name the name of the execution thread.
     * @param task the task to be executed in the new thread.
     * @return the <code>WorkerTask</code> wrapper to control start and
     *   termination of the thread.
     */
    public WorkerTask createWorkerThread(String name, WorkerTask task,
            boolean daemon) {
        WorkerThread wt = new WorkerThread(name, task);
        wt.thread.setDaemon(daemon);
        logger.trace(Util.delayedFormatString("New SNMP thread: %s", name));
        return wt;
    }

    /**
     * Sets the maximum time to wait when joining a worker task thread.
     * @param millis
     *    the time to wait. 0 waits forever.
     * @since 1.10.2
     */
    public void setThreadJoinTimeout(long millis) {
        this.joinTimeout = millis;
    }

    public class WorkerThread implements WorkerTask {

        private Thread thread;
        private WorkerTask task;
        private boolean started = false;

        public WorkerThread(final String name, WorkerTask task) {
            this.thread = new Thread(task, name);
            this.thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    logger.fatal(String.format("Thread %s died", name));
                    Writer w = new CharArrayWriter(e.getStackTrace().length + 20);
                    PrintWriter p = new PrintWriter(w);
                    p.println("Error stack: ");
                    do {
                        p.println("  " + e.getMessage());
                        for(StackTraceElement l: e.getStackTrace()) {
                            p.println("    at " + l.toString());
                        }
                        e = e.getCause();
                    } while(e != null);
                    p.flush();
                    logger.fatal(w);     
                }
            });
            this.task = task;
        }

        public void terminate() {
            task.terminate();
        }

        public void join() throws InterruptedException {
            task.join();
            thread.join(joinTimeout);
        }

        public void run() {
            if (!started) {
                started = true;
                thread.start();
            }
            else {
                thread.run();
            }
        }

        public void interrupt() {
            task.interrupt();
            thread.interrupt();
        }
    }
}
