package jrds;

import java.io.IOException;

import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdNioBackendFactory;

public class RrdAccountingNioBackendFactory extends RrdNioBackendFactory {
	/** factory name, "NIO" */
	public static final String NAME = "ACCNIO";

	/**
	 * Period in seconds between consecutive synchronizations when
	 * sync-mode is set to SYNC_BACKGROUND. By default in-memory cache will be
	 * transferred to the disc every 300 seconds (5 minutes). Default value can be
	 * changed via {@link #setSyncPeriod(int)} method.
	 */
	public static final int DEFAULT_SYNC_PERIOD = 300; // seconds

	private static int syncPeriod = DEFAULT_SYNC_PERIOD;

	/**
	 * Returns time between two consecutive background synchronizations. If not changed via
	 * {@link #setSyncPeriod(int)} method call, defaults to {@link #DEFAULT_SYNC_PERIOD}.
	 * See {@link #setSyncPeriod(int)} for more information.
	 * @return Time in seconds between consecutive background synchronizations.
	 */
	public static int getSyncPeriod() {
		return syncPeriod;
	}

	/**
	 * Sets time between consecutive background synchronizations.
	 * @param syncPeriod Time in seconds between consecutive background synchronizations.
	 */
	public static void setSyncPeriod(int syncPeriod) {
		RrdAccountingNioBackendFactory.syncPeriod = syncPeriod;
	}

	/**
	 * Creates RrdNioBackend object for the given file path.
	 * @param path File path
	 * @param readOnly True, if the file should be accessed in read/only mode.
	 * False otherwise.
	 * @return RrdNioBackend object which handles all I/O operations for the given file path
	 * @throws IOException Thrown in case of I/O error.
	 */
	protected RrdBackend open(String path, boolean readOnly) throws IOException {
		return new RrdAccountingNioBackend(path, readOnly, null, syncPeriod);
	}

	/**
	 * Returns the name of this factory.
	 * @return Factory name (equals to string "NIO")
	 */
	public String getFactoryName() {
		return NAME;    
	}

}
