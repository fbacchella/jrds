package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jrds.Util;

import org.apache.log4j.Logger;

public class PageCache {
    static final private Logger logger = Logger.getLogger(PageCache.class);

    public final static int PAGESIZE = 4192;
    private final ConcurrentMap<String, Map<Long, Integer>> files = new ConcurrentHashMap<String, Map<Long, Integer>>();
    private final LRUMap<Integer, FilePage> pagecache;
    private final ByteBuffer pagecacheBuffer;
    private final Timer syncTimer = new Timer(true);

    public PageCache(int maxObjects, int syncPeriod) {
        pagecacheBuffer = ByteBuffer.allocateDirect(maxObjects * PAGESIZE);

        //Create the page cache in memory
        pagecache = new LRUMap<Integer, FilePage>(maxObjects);
        //And fill it with empty pages
        for(int i=2; i < maxObjects; i++ ) {
            pagecache.put(i, new FilePage(pagecacheBuffer, i));
        }

        //createSyncTask(syncPeriod);
        logger.info(Util.delayedFormatString("created a page cache with %d %d pages, using %d of memory", maxObjects, PAGESIZE, maxObjects * PAGESIZE));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        sync();
    }

    private void createSyncTask(int syncPeriod) {
        TimerTask syncTask = new TimerTask() {
            public void run() {
                PageCache.this.sync();
            }
        };
        syncTimer.schedule(syncTask, syncPeriod * 1000L, syncPeriod * 1000L);
    }

    private FilePage find(File file, long offset) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        FilePage page = null;
        Map<Long, Integer> m1 = files.get(canonicalPath);
        if(m1 == null) {
            m1 = new HashMap<Long, Integer>();
            files.putIfAbsent(canonicalPath, m1);
        }

        long offsetPage = (long) (Math.floor(offset / PAGESIZE) * PAGESIZE);
        synchronized(this){
            m1 = files.get(canonicalPath);
            Integer index = m1.get(offsetPage);
            if(index == null) {
                page = pagecache.removeEldest();
                if(! page.isEmpty()) {
                    files.get(page.filepath).remove(page.pageIndex);
                    page.free();
                }
                index = page.pageIndex;

                page.load(file, offsetPage);
                pagecache.put(index, page);
                m1.put(offsetPage, index);
                logger.debug(Util.delayedFormatString("Loading page %d from %s", offset, file.getCanonicalPath()));
            }
            else 
                page = pagecache.get(index);
        }

        return page;
    }

    public void read(File file, long offset, byte[] buffer) throws IOException {
        logger.trace(Util.delayedFormatString("Loading %d bytes at offset %d from %s", buffer.length, offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = (long) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        long cacheEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
        while(cacheStart <= cacheEnd) {
            FilePage current = find(file, cacheStart);
            current.read(offset, buffer);
            cacheStart += PAGESIZE;
        }
    }

    public void write(File file, long offset, byte[] buffer) throws IOException {
        logger.trace(Util.delayedFormatString("Writing %d bytes at offset %d to %s", buffer.length, offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = (long) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        long cacheEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
        while(cacheStart <= cacheEnd) {
            logger.trace(Util.delayedFormatString("Writting at page %d", cacheStart));
            FilePage current = find(file, cacheStart);
            current.write(offset, buffer);
            cacheStart += PAGESIZE;
        }

    }

    public void sync() {
        for(FilePage p: pagecache.values()) {
            try {
                p.sync();
            } catch (IOException e) {
                logger.error(Util.delayedFormatString("sync failed for %s:", p.filepath, e));
            }
        }
    }

}
