package jrds;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdFileBackendFactory;

/**
 * A wrapper classe, to manage the rrdDb operations
 */
public final class StoreOpener {
    static final private Logger logger = Logger.getLogger(StoreOpener.class);

    private static RrdDbPool instance = null;

    private static final AtomicLong waitTime = new AtomicLong(0);
    private static final AtomicInteger lockCount = new AtomicInteger(0);
    private static String backend = null;
    private static Boolean usepool = null;

    /**
     * Retrieves the RrdDb instance matching a specific RRD datasource name
     * (usually a file name) and using a specified RrdBackendFactory.
     *
     * @param rrdFile Name of the RRD datasource.
     * @return RrdDb instance of the datasource.
     * @throws IOException Thrown in case of I/O error.
     * @throws InterruptedException 
     */
    public final static RrdDb getRrd(String rrdFile)
            throws IOException {
        File f = new File(rrdFile);
        String cp = f.getCanonicalPath();
        long start = System.currentTimeMillis();
        RrdDb db;
        if(usepool)
            db = instance.requestRrdDb(cp);
        else
            db = new RrdDb(cp);
        long finish = System.currentTimeMillis();
        waitTime.addAndGet(finish - start);
        lockCount.incrementAndGet();
        return db;
    }

    /**
     * @param db
     */
    public final static void releaseRrd(RrdDb db)  {
        try {
            long start = System.currentTimeMillis();
            if(usepool)
                instance.release(db);
            else
                db.close();
            long finish = System.currentTimeMillis();
            waitTime.addAndGet(finish - start);
            lockCount.incrementAndGet();
        } catch (Exception e) {
            logger.debug("Strange error " + e);
        }
    }

    public static final void prepare() {
        prepare(null);
    }
    
    public static final void prepare(String backend) {
        usepool = false;
        if(backend !=  null & StoreOpener.backend != null)
            RrdBackendFactory.setDefaultFactory(backend);
        else if(StoreOpener.backend != null && ! StoreOpener.backend.equals(backend))
            logger.warn("Trying to change backend, a restart is needed");
    }
    
    public static final void prepare(int dbPoolSize, int syncPeriod, int timeout, String backend) {
        if(backend !=  null & StoreOpener.backend != null)
            RrdBackendFactory.setDefaultFactory(backend);
        else if(StoreOpener.backend != null && ! StoreOpener.backend.equals(backend))
            logger.warn("Trying to change backend, a restart is needed");
        
        RrdBackendFactory factory = RrdBackendFactory.getDefaultFactory();
        if(factory instanceof RrdFileBackendFactory && instance == null) {
            instance = RrdDbPool.getInstance();
            instance.setCapacity(dbPoolSize);
            usepool = true;
        }
    }

    public static final void stop() {
        RrdBackendFactory factory = RrdBackendFactory.getDefaultFactory();
        if(factory instanceof RrdAccountingNioBackendFactory) {
            logger.info("backend opened: " + RrdAccountingNioBackend.getAccess());
            logger.info("backend bytes read: " + RrdAccountingNioBackend.getBytesRead());
            logger.info("backend reads: " + RrdAccountingNioBackend.getReadOp());
            logger.info("backend bytes written: " + RrdAccountingNioBackend.getBytesWritten());
            logger.info("backend reads: " + RrdAccountingNioBackend.getWriteOp());
        }
        logger.info("Average wait time: " +  waitTime.doubleValue() / lockCount.doubleValue() + " ms");

    }

    public static final void reset() {
    }

    /**
     * @return the instance
     */
    public static RrdDbPool getInstance() {
        return instance;
    }

    /**
     * @param rrdDb
     * @return
     * @throws IOException
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(org.rrd4j.core.RrdDb)
     */
    public static int getOpenCount(RrdDb rrdDb) throws IOException {
        return instance.getOpenCount(rrdDb);
    }

    /**
     * @param path
     * @return
     * @throws IOException
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(java.lang.String)
     */
    public static int getOpenCount(String path) throws IOException {
        return instance.getOpenCount(path);
    }
}
