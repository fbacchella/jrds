package jrds;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdFileBackendFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper classe, to manage the rrdDb operations
 */
public final class StoreOpener {
    static final private Logger logger = LoggerFactory.getLogger(StoreOpener.class);

    private static RrdDbPool instance = null;

    private static final AtomicLong waitTime = new AtomicLong(0);
    private static final AtomicInteger lockCount = new AtomicInteger(0);
    private static RrdBackendFactory backend;
    private static boolean usepool = false;

    /**
     * Retrieves the RrdDb instance matching a specific RRD datasource name
     * (usually a file name) and using a specified RrdBackendFactory.
     *
     * @param rrdFile Name of the RRD datasource.
     * @return RrdDb instance of the datasource.
     * @throws IOException Thrown in case of I/O error.
     */
    public static RrdDb getRrd(String rrdFile) throws IOException {
        File f = new File(rrdFile);
        String cp = f.getCanonicalPath();
        long start = System.currentTimeMillis();
        RrdDb db = RrdDb.getBuilder().setUsePool(usepool).setBackendFactory(backend).setPath(cp).build();
        long finish = System.currentTimeMillis();
        waitTime.addAndGet(finish - start);
        lockCount.incrementAndGet();
        return db;
    }

    /**
     * @param db
     */
    public static void releaseRrd(RrdDb db) {
        try {
            long start = System.currentTimeMillis();
            db.close();
            long finish = System.currentTimeMillis();
            waitTime.addAndGet(finish - start);
            lockCount.incrementAndGet();
        } catch (Exception ex) {
            logger.debug("Strange error {}", Util.delayedFormatString(() -> Util.resolveThrowableException(ex)));
        }
    }

    @SuppressWarnings("deprecation")
    public static void prepare(String backend) {
        usepool = false;
        if(backend != null) {
            StoreOpener.backend = RrdBackendFactory.getFactory(backend);
            logger.trace("Store backend set to {}", backend);
        } else
            StoreOpener.backend = RrdBackendFactory.getDefaultFactory();

        logger.debug("Store backend used is {}", StoreOpener.backend.getName());
        logger.debug("use pool: {}", usepool);
    }

    @SuppressWarnings("deprecation")
    public static void prepare(String backend, int dbPoolSize) {
        usepool = false;
        if(backend != null) {
            try {
                RrdBackendFactory.setDefaultFactory(backend);
                logger.trace("Store backend set to {}", backend);
            } catch (IllegalArgumentException e) {
                logger.error("Backend not configured: {}", e.getMessage());
            } catch (IllegalStateException e) {
                logger.warn("Trying to change default backend, a restart is needed");
            }
        }
        StoreOpener.backend = RrdBackendFactory.getDefaultFactory();

        if(StoreOpener.backend instanceof RrdFileBackendFactory && dbPoolSize != 0) {
            try {
                instance = RrdDbPool.getInstance();
                instance.setCapacity(dbPoolSize);
                usepool = true;
            } catch (Exception e) {
            }
        }
        logger.debug("Store backend used is {}", StoreOpener.backend.getName());
        logger.debug("use pool: {} {}", usepool, dbPoolSize);
    }

    public static void stop() {
        logger.info("Average wait time: {} ms", waitTime.doubleValue() / lockCount.doubleValue());
    }

    @Deprecated
    public static void reset() {
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
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(org.rrd4j.core.RrdDb)
     */
    public static int getOpenCount(RrdDb rrdDb) {
        return instance.getOpenCount(rrdDb);
    }

    /**
     * @param path
     * @return
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(java.lang.String)
     */
    public static int getOpenCount(String path) {
        return instance.getOpenCount(path);
    }
}
