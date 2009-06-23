package jrds;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.rrd4j.core.RrdNioBackend;

public class RrdAccountingNioBackend extends RrdNioBackend {
	protected RrdAccountingNioBackend(String path, boolean readOnly,
			ScheduledExecutorService syncExecutor, int syncPeriod)
			throws IOException {
		super(path, readOnly, syncExecutor, syncPeriod);
		// TODO Auto-generated constructor stub
	}

	private static final AtomicInteger bytesRead = new AtomicInteger(0);
	private static final AtomicInteger readOp = new AtomicInteger(0);

	private static final AtomicInteger bytesWritten = new AtomicInteger(0);
	private static final AtomicInteger writeOp = new AtomicInteger(0);
	private static final AtomicInteger access = new AtomicInteger(0);

	/*protected RrdAccountingNioBackend(String path, boolean readOnly, int syncPeriod)
	throws IOException {
		super(path, readOnly, syncPeriod);
		access.incrementAndGet();
	}*/

	@Override
	protected synchronized void read(long offset, byte[] b) throws IOException {
		super.read(offset, b);
		bytesRead.addAndGet(b.length);
		readOp.incrementAndGet();
	}

	@Override
	protected synchronized void write(long offset, byte[] b) throws IOException {
		super.write(offset, b);
		bytesWritten.addAndGet(b.length);
		writeOp.incrementAndGet();
	}

	@Override
	protected synchronized void sync() {
	}

	public static int getAccess() {
		return access.get();
	}

	public static int getBytesRead() {
		return bytesRead.get();
	}

	public static int getBytesWritten() {
		return bytesWritten.get();
	}
	
	public static int getReadOp() {
		return readOp.get();
	}
	
	public static int getWriteOp() {
		return writeOp.get();
	}
}
