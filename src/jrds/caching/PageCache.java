package jrds.caching;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public class PageCache {

    public final static int PAGESIZE = 4192;
    private final Map<FileChannel, Map<Long, Integer>> files = new HashMap<FileChannel, Map<Long, Integer>>();
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
    private synchronized void remove(Integer pageIndex, boolean removeFromLRU){
        FilePage page = pagecache.get(pageIndex);
        try {
            page.sync();
            files.get(page.file).remove(pageIndex);
            if(removeFromLRU)
                pagecache.remove(pageIndex);
            freePages.add(pageIndex);
        } catch (IOException e) {
        }
    }
    
    private synchronized FilePage find(FileChannel file, long offset) throws IOException {
        FilePage page = null;
        Map<Long, Integer> m1 = files.get(file);
        if(m1 == null) {
            m1 = new HashMap<Long, Integer>();
            files.put(file, m1);
        }
        
        long offsetPage = offset % PAGESIZE;
        Integer index = m1.get(offsetPage);
        if(index == null) {
            Integer firstFreeIndex = freePages.pollFirst();
            page = new FilePage(pagecacheBuffer, firstFreeIndex, file, offset);
            pagecache.put(firstFreeIndex, page);
        }
        else 
            page = pagecache.get(index);
        
        return page;
    }

    public void read(FileChannel channel, byte[] buffer, long offset) throws IOException {
        if(buffer.length == 0)
            return;
        
        long cacheStart = (long) (Math.floor( offset /  PAGESIZE) * PAGESIZE);
        long cacheEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PAGESIZE) * PAGESIZE);
        while(cacheEnd > cacheStart) {
            FilePage current = find(channel, cacheStart);
            current.read(buffer, offset);
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
