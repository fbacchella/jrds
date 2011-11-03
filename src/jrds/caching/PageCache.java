package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jrds.Util;

import org.apache.log4j.Logger;

public class PageCache {
    static final private Logger logger = Logger.getLogger(PageCache.class);

    public final static int PAGESIZE = 4096;
    private final ConcurrentMap<String, Map<Long, Integer>> files = new ConcurrentHashMap<String, Map<Long, Integer>>();
    private final LRUArray<FilePage> pagecache;
    private final ByteBuffer pagecacheBuffer;
    private final Timer syncTimer = new Timer(true);

    public PageCache(int maxObjects, int syncPeriod) {
        pagecacheBuffer = ByteBuffer.allocateDirect(maxObjects * PAGESIZE);

        //Create the page cache in memory
        pagecache = new LRUArray<FilePage>(maxObjects);
        //And fill it with empty pages
        for(int i=0; i < maxObjects; i++ ) {
            pagecache.put(i, new FilePage(pagecacheBuffer, i));
        }

        createSyncTask(syncPeriod);
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
            m1 = new TreeMap<Long, Integer>();
            files.putIfAbsent(canonicalPath, m1);
        }

        long offsetPage = (long) (Math.floor(offset / PAGESIZE) * PAGESIZE);
        synchronized(this){
            m1 = files.get(canonicalPath);
            Integer index = m1.get(offsetPage);
            if(index == null) {
                //page is remove from the page cache, but it will be put back
                page = pagecache.removeEldest();
                if(! page.isEmpty()) {
                    final String filepath = page.filepath;
                    //Remove page from page used by this file
                    files.get(filepath).remove(page.fileOffset);
                    //Launch a synchronization thread if needed for this file, to keep it on a coherent state on disk
                    if(page.isDirty()) {
                        Thread syncthread = new Thread() {
                            @Override
                            public void run() {
                                Map<Long, Integer> p = PageCache.this.files.get(filepath);
                                for(Integer index: p.values()) {
                                    FilePage page = PageCache.this.pagecache.getQuiet(index);
                                    try {
                                        page.sync();
                                    } catch (IOException e) {
                                        logger.error(Util.delayedFormatString("sync failed for %s:", page.filepath, e));
                                    }
                                }
                            }
                        };
                        syncthread.setDaemon(true);
                        syncthread.start();
                    }
                    page.free();
                }
                index = page.pageIndex;

                page.load(file, offsetPage);
                pagecache.put(index, page);
                m1.put(offsetPage, index);
                logger.debug(Util.delayedFormatString("Loading at offset %d from %s", offsetPage, file.getCanonicalPath()));
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

        int cacheStart = (int) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        int cacheEnd = (int) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
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

        int cacheStart = (int) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        int cacheEnd = (int) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
        while(cacheStart <= cacheEnd) {
            logger.trace(Util.delayedFormatString("Writting at page %d", cacheStart));
            FilePage current = find(file, cacheStart);
            current.write(offset, buffer);
            cacheStart += PAGESIZE;
        }

    }

    public void sync() {
        for( Map<Long, Integer> p: files.values()) {
            for(Integer index: p.values()) {
                FilePage page = pagecache.getQuiet(index);
                try {
                    page.sync();
                } catch (IOException e) {
                    logger.error(Util.delayedFormatString("sync failed for %s:", page.filepath, e));
                }
            }
        }
    }

}
