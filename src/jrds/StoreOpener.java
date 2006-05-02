package jrds;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
/**
 * A wrapper classe, to manage the rrdDb operations
 */
public final class StoreOpener {
	static final private Logger logger = Logger.getLogger(StoreOpener.class);
	static final RrdDbPool pool = RrdDbPool.getInstance();
	
	//static final private RrdOpener opener = new RrdOpener(true);
	
    /**
     * Retrieves the RrdDb instance matching a specific RRD datasource name
     * (usually a file name) and using a specified RrdBackendFactory.
     *
     * @param rrdFile Name of the RRD datasource.
     * @return RrdDb instance of the datasource.
     * @throws IOException Thrown in case of I/O error.
     * @throws RrdException Thrown in case of a JRobin specific error.
     */
	public final static RrdDb getRrd(String rrdFile)
	throws IOException {
		return pool.requestRrdDb(rrdFile);
	}
	/**
	 * @param arg0
	 */
	public final static void releaseRrd(RrdDb arg0)  {
		try {
			pool.release(arg0);
		} catch (Exception e) {
			logger.debug("Strange error" + e);
		}
	}
	
	public static final void prepare(int dbPoolSize, int syncPeriod) {
		RrdDbPool.getInstance().setCapacity(dbPoolSize);
		
		RrdCachedFileBackendFactory.setSyncMode(RrdCachedFileBackendFactory.SYNC_CENTRALIZED);
		if(syncPeriod > 0)
			RrdCachedFileBackendFactory.setSyncPeriod(syncPeriod);
		RrdBackendFactory.registerAndSetAsDefaultFactory(new RrdCachedFileBackendFactory());
	}
	
	public static final void stop() {
		//RrdDbPool dbpool = RrdDbPool.getInstance();
		//logger.info("RrdDbPool efficiency: " + dbpool.getPoolEfficency());
		//logger.info("RrdDbPool hits: " + dbpool.getPoolHitsCount());
		//logger.info("RrdDbPool requets: " + dbpool.getPoolRequestsCount());
		
		/*try {
			dbpool.reset();
		} catch (IOException e) {
			logger.error("Strange problem while stopping db pool: ", e);
		}*/
		
		logger.info("Cached backend efficiency: " + RrdCachedFileBackend.getCacheEfficency());
		logger.info("Cached backend  hits: " + RrdCachedFileBackend.getCacheHitsCount());
		logger.info("Cached backend requests: " + RrdCachedFileBackend.getCacheRequestsCount());
		
	}
	
	
}
