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
		RrdDb db = instance.requestRrdDb(cp);
		long finish = System.currentTimeMillis();
		waitTime.addAndGet(finish - start);
		lockCount.incrementAndGet();
		return db;
	}

	/**
	 * @param arg0
	 */
	public final static void releaseRrd(RrdDb arg0)  {
		try {
			long start = System.currentTimeMillis();
			instance.release(arg0);
			long finish = System.currentTimeMillis();
			waitTime.addAndGet(finish - start);
			lockCount.incrementAndGet();
		} catch (Exception e) {
			logger.debug("Strange error " + e);
		}
	}

	public static final void prepare(int dbPoolSize, int syncPeriod, int timeout, String backend) {
		if(instance == null) {
			instance = RrdDbPool.getInstance();
			instance.setCapacity(dbPoolSize);
			if(backend !=  null)
				RrdBackendFactory.setDefaultFactory(backend);
		}
		RrdBackendFactory factory = RrdBackendFactory.getDefaultFactory();

		if (!(factory instanceof RrdFileBackendFactory)) {
			throw new RuntimeException("Cannot create an opener with " +
			"a default backend factory not derived from RrdFileBackendFactory");
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
		logger.info("Average wait time: " +  waitTime.doubleValue() / lockCount.doubleValue());

	}

	public static final void reset() {
	}

	/**
	 * @return the instance
	 */
	public static RrdDbPool getInstance() {
		return instance;
	}
}
