/*
 * Created on 17 juil. 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;


import org.apache.log4j.Logger;
import org.jrobin.core.RrdFileBackend;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RrdCachedFileBackend extends RrdFileBackend {
	static final private Logger logger = JrdsLogger.getLogger(RrdCachedFileBackend.class);
	private static final Timer syncTimer = new Timer(true);

	private static final int CACHE_LENGTH = 8192;
	ByteBuffer byteBuffer;
	private int cacheSize;
	private long cacheStart;
	private boolean cacheDirty = false;
	private int dirtyStart;
	private int dirtyEnd;

	private int syncMode;
	private TimerTask syncTask;
	
	private int writeHit = 0;
	private int readHit = 0;
	private int access = 0;

	/**
	 * @param path
	 */
	public RrdCachedFileBackend(String path, boolean readOnly, int lockMode, int syncMode, int syncPeriod)
		throws IOException {
			super(path, readOnly, lockMode);
			this.syncMode = syncMode;
			if(syncMode == RrdCachedFileBackendFactory.SYNC_BACKGROUND && !readOnly) {
				createSyncTask(syncPeriod);
			}
	}

	private void createSyncTask(int syncPeriod) {
		syncTask = new TimerTask() {
			public void run() {
				sync();
			}
		};
		syncTimer.schedule(syncTask, syncPeriod * 1000L, syncPeriod * 1000L);
	}

	/**
	 * This method forces all data cached in memory but not yet stored in the file,
	 * to be stored in it. RrdNioBackend uses (a lot of) memory to cache I/O data.
	 * This method is automatically invoked when the {@link #close()}
	 * method is called. In other words, you don't have to call sync() before you call close().<p>
	 */
	public void sync() {
		if(cacheDirty) {
			synchronized(byteBuffer) {
				logger.debug("** SYNC **");
				try {
					final int oldPosition = byteBuffer.position();
					final int oldLimit = byteBuffer.limit();
					byteBuffer.position(dirtyStart);
					byteBuffer.limit(dirtyEnd);
					logger.debug(byteBuffer);
					logger.debug(channel.write(byteBuffer, cacheStart + dirtyStart) + " bytes written");
					byteBuffer.position(oldPosition);
					byteBuffer.limit(oldLimit);
					cacheDirty = false;
					dirtyStart = byteBuffer.capacity();
					dirtyEnd = 0;
				} catch (IOException e) {
					logger.error("Panic whils syncing " +  this.getPath() + ": " + e);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jrobin.core.RrdBackend#write(long, byte[])
	 */
	public void write(long offset, byte[] b) throws IOException {
		access++;
		//boolean todo = true;
		final long cacheEnd = cacheStart + cacheSize;
		if(byteBuffer == null || offset < cacheStart || offset + b.length > cacheEnd)
			read(offset, new byte[b.length]);
		else
			writeHit++;
		
		cacheDirty = true;
		byteBuffer.position((int)(offset - cacheStart));
		byteBuffer.put(b);
		dirtyStart = Math.min((int)(offset - cacheStart), dirtyStart);
		dirtyEnd = Math.max(dirtyEnd, byteBuffer.position());
	}
	
	/**
	 * Reads a number of bytes from the RRD file on the disk
	 * @param offset Starting file offset
	 * @param b Buffer which receives bytes read from the file.
	 * @throws IOException Thrown in case of I/O error.
	 */
	public void read(long offset, byte[] b) throws IOException {
		access++;
		boolean todo = true;
		final long cacheEnd = cacheStart + cacheSize;
		if(offset >= cacheStart && offset + b.length < cacheEnd)
			todo = false;
		
		if(todo) {
			if(cacheDirty)
				sync();
			cacheSize = (int) (Math.ceil((float)b.length /  CACHE_LENGTH) * CACHE_LENGTH);
			cacheStart = (int) (Math.floor((float)offset /  CACHE_LENGTH) * CACHE_LENGTH);
			logger.debug("Will read " + cacheSize + " bytes at position " + cacheStart);
			byteBuffer = ByteBuffer.allocate(cacheSize);
			channel.position(cacheStart);
			cacheDirty = false;
			dirtyStart = byteBuffer.capacity();
			dirtyEnd = 0;
			if( channel.read(byteBuffer) < b.length) {
				throw new IOException("Not enough bytes available in file " + getPath());
			}
		}
		else {
			readHit++;
		}
		byteBuffer.position((int)(offset - cacheStart));
		byteBuffer.get(b);
	}
	/* (non-Javadoc)
	 * @see org.jrobin.core.RrdBackend#close()
	 */
	public void close() throws IOException {
		super.close();
		logger.debug("Access: " + access);
		logger.debug("Write hit: " + writeHit);
		logger.debug("Read hit: " + readHit);
	}
}
