package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import jrds.Util;

public class PageCache {
    static final private Logger logger = Logger.getLogger(PageCache.class);

    public final static int PAGESIZE = 4192;
    private final Map<String, Map<Long, Integer>> files = new HashMap<String, Map<Long, Integer>>();
    private final LRUMap<Integer, FilePage> pagecache;
    private final ByteBuffer pagecacheBuffer;
    private final TreeSet<Integer> freePages;
    private final Timer syncTimer = new Timer(true);

    public PageCache(int maxObjects, int syncPeriod) {
        pagecacheBuffer = ByteBuffer.allocateDirect(maxObjects * PAGESIZE);
        freePages =  new TreeSet<Integer>();
        for(int i=0; i< maxObjects; i++ ) {
            freePages.add(i);
        }

        pagecache = new LRUMap<Integer, FilePage>(maxObjects) {
            /* (non-Javadoc)
             * @see jrds.caching.LRUMap#processRemovedLRU(java.lang.Object, java.lang.Object)
             */
            @Override
            protected void processRemovedLRU(Integer key, FilePage value) {
                PageCache.this.remove(key, false);
                super.processRemovedLRU(key, value);
            }

        };
        createSyncTask(syncPeriod);
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

    /**
     * @param pageIndex
     * @param removeFromLRU true if it's needed to remove the page from the boolean as this method can be called from inside the LRU
     */
    private void remove(Integer pageIndex, boolean removeFromLRU){
        FilePage page = pagecache.get(pageIndex);
        try {
            page.sync();
            files.get(page.filepath).remove(pageIndex);
            if(removeFromLRU)
                pagecache.remove(pageIndex);
            freePages.add(pageIndex);
        } catch (IOException e) {
        }
    }

    private FilePage find(File file, long offset) throws IOException {
        FilePage page = null;
        Map<Long, Integer> m1 = files.get(file.getCanonicalFile());
        if(m1 == null) {
            m1 = new HashMap<Long, Integer>();
            files.put(file.getCanonicalPath(), m1);
        }

        long offsetPage = offset % PAGESIZE;
        Integer index = m1.get(offsetPage);
        if(index == null) {
            Integer firstFreeIndex = freePages.pollFirst();
            page = new FilePage(pagecacheBuffer, firstFreeIndex, file, offset);
            pagecache.put(firstFreeIndex, page);
            logger.debug(Util.delayedFormatString("Loading page %d from %s", offset, file.getCanonicalPath()));
        }
        else 
            page = pagecache.get(index);

        return page;
    }

    public void read(File file, long offset, byte[] buffer) throws IOException {
        logger.debug(Util.delayedFormatString("Loading bytes %d from %s", offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = (long) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        long cacheEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
        logger.debug(Util.delayedFormatString("from %d to %d", cacheStart, cacheEnd));
        while(cacheEnd >= cacheStart) {
            FilePage current = find(file, cacheStart);
            current.read(offset, buffer);
            cacheStart += PAGESIZE;
        }
    }

    public void write(File file, long offset, byte[] buffer) throws IOException {
        if(buffer.length == 0)
            return;

        long cacheStart = (long) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        long cacheEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
        while(cacheEnd > cacheStart) {
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
            }
        }
    }

}
