package jrds;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdFileBackendFactory;

/**
 * A wrapper classe, to manage the rrdDb operations
 */
public final class StoreOpener {
	static final private Logger logger = Logger.getLogger(StoreOpener.class);

	static private class Indirection {
		RrdDb db = null;
		int count = 0;
	}

	public static final int INITIAL_CAPACITY = 200;
	private static Semaphore capacity;
	private static int timeout;

	static private final ConcurrentHashMap<String, Indirection> pool = new ConcurrentHashMap<String, Indirection>(INITIAL_CAPACITY);

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
	 * @throws RrdException Thrown in case of a JRobin specific error.
	 */
	public final static RrdDb getRrd(String rrdFile)
	throws IOException, InterruptedException {
		File f = new File(rrdFile);
		String cp = f.getCanonicalPath();
		long start = System.currentTimeMillis();
		lockCount.incrementAndGet();
		Indirection ref = pool.putIfAbsent(cp, new Indirection());
		if(ref == null)
			ref = pool.get(cp);
		synchronized(ref) {
			if(ref.db == null) {
				if(capacity.availablePermits() <= 0) {
					logger.warn("Too much rrd opened, do you have a leak ?");
				}
				capacity.tryAcquire(timeout, TimeUnit.SECONDS);
				ref.db = new RrdDb(rrdFile);
			}
			ref.count++;
		}
		long finish = System.currentTimeMillis();
		waitTime.addAndGet(finish - start);
		return ref.db;
	}

	/**
	 * @param arg0
	 */
	public final static void releaseRrd(RrdDb arg0)  {
		try {
			long start = System.currentTimeMillis();
			lockCount.incrementAndGet();
			Indirection ref = pool.get(arg0.getCanonicalPath());
			if(ref != null) {
				synchronized(ref) {
					if(ref.db != null) {
						ref.count--;
						if(ref.count <= 0) {
							ref.db.close();
							ref.db = null;
							pool.remove(ref);
							capacity.release();
						}
					}
				}
			}
			long finish = System.currentTimeMillis();
			waitTime.addAndGet(finish - start);
		} catch (Exception e) {
			logger.debug("Strange error " + e);
		}
	}

	public static final void prepare(int dbPoolSize, int syncPeriod, int timeout, String backend) {
		RrdBackendFactory.registerFactory(new RrdAccountingNioBackendFactory());
		RrdBackendFactory.setDefaultFactory(backend);

		RrdBackendFactory factory = RrdBackendFactory.getDefaultFactory();

		if (!(factory instanceof RrdFileBackendFactory)) {
			throw new RuntimeException("Cannot create an opener with " +
			"a default backend factory not derived from RrdFileBackendFactory");
		}

		capacity = new Semaphore(dbPoolSize);

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
}
