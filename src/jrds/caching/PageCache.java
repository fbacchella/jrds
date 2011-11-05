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

    public final static int PAGESIZE = 512;
    private final ConcurrentMap<String, Map<Long, FilePage>> files = new ConcurrentHashMap<String, Map<Long, FilePage>>();
    final LRUArray<FilePage> pagecache;
    final ByteBuffer pagecacheBuffer;
    private final Timer syncTimer = new Timer(true);

    public PageCache(int maxObjects, int syncPeriod) {
        pagecacheBuffer = ByteBuffer.allocateDirect(maxObjects * PAGESIZE);

        //Create the page cache in memory
        pagecache = new LRUArray<FilePage>(maxObjects);
        //And fill it with empty pages
        for(int i = 0 ; i < maxObjects ; i++ ) {
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
        Map<Long, FilePage> m1 = files.get(canonicalPath);
        if(m1 == null) {
            m1 = new TreeMap<Long, FilePage>();
            files.putIfAbsent(canonicalPath, m1);
        }

        long offsetPage = offsetPage(offset);
        FilePage page = null;
        synchronized(this){
            m1 = files.get(canonicalPath);
            page = m1.get(offsetPage);
            // Page is not cached
            // we need to free an old one and use it
            if(page == null) {
                //page is remove from the page cache, but it will be put back
                page = pagecache.removeEldest();
                //We getting an already used page, it needs to be clean before reuse
                if(! page.isEmpty()) {
                    logger.trace(Util.delayedFormatString("Flushing page %d, used by file %s at offset %d", page.pageIndex, page.filepath, page.pageIndex));
                    final String filepath = page.filepath;
                    //Remove page from page used by this file
                    files.get(filepath).remove(page.fileOffset);
                    page.free();
                    //Launch a synchronization thread if needed for this file, to keep it on a coherent state on disk
                    if(page.isDirty()) {
                        Thread syncthread = new Thread() {
                            @Override
                            public void run() {
                                Map<Long, FilePage> pages = PageCache.this.files.get(filepath);
                                for(FilePage page: pages.values()) {
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
                }

                page.load(file, offsetPage);
                pagecache.put(page.pageIndex, page);
                m1.put(offsetPage, page);
                logger.trace(Util.delayedFormatString("Loading at offset %d from %s in page %d", offsetPage, page.filepath, page.pageIndex));
            }
        }

        return page;
    }

    public void read(File file, long offset, byte[] buffer) throws IOException {
        logger.debug(Util.delayedFormatString("Loading %d bytes at offset %d from %s", buffer.length, offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = offsetPage(offset);
        long cacheEnd = offsetPage(offset + buffer.length - 1);
        while(cacheStart <= cacheEnd) {
            FilePage current = find(file, cacheStart);
            current.read(offset, buffer);
            cacheStart += PAGESIZE;
        }
    }

    public void write(File file, long offset, byte[] buffer) throws IOException {
        logger.debug(Util.delayedFormatString("Writing %d bytes at offset %d to %s", buffer.length, offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = offsetPage(offset);
        long cacheEnd = offsetPage(offset + buffer.length - 1);
        while(cacheStart <= cacheEnd) {
            FilePage current = find(file, cacheStart);
            current.write(offset, buffer);
            cacheStart += PAGESIZE;
        }
    }

    public void sync() {
        for( Map<Long, FilePage> p: files.values()) {
            //                    FilePage[] pages = new FilePage[p.size()];
            //                    //Need to run on a copy, to avoid concurent modifications;
            //                    synchronized(p) {
            //                        int i=0 ;
            //                        for(FilePage page: p.values()) {
            //                            pages[i++] = page;
            //                        }
            //                    }
            for(FilePage page:p.values()) {
                try {
                    page.sync();
                } catch (IOException e) {
                    logger.error(Util.delayedFormatString("sync failed for %s:", page.filepath, e));
                }
            }
        }
    }

    static final long offsetPage(long offset) {
        return offset - ( offset %  PAGESIZE );
    }
}
