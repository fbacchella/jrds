package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jrds.Util;

import org.apache.log4j.Logger;

class PageCache {
    static final private Logger logger = Logger.getLogger(PageCache.class);
    //flag to detect Linux, because of directio alignment problems
    static final boolean isLinux = "Linux".matches(System.getProperty("os.name"));

    static final private class FileInfo {
        final Map<Long, FilePage> pagesList = new ConcurrentSkipListMap<Long, FilePage>();
        final ReadWriteLock lock = new ReentrantReadWriteLock(); 
    }

    native static int getAlignOffset(ByteBuffer buffer);

    public final static int PAGESIZE = 4096;
    private final ConcurrentMap<String, FileInfo> files = new ConcurrentHashMap<String, FileInfo>();
    final LRUArray<FilePage> pagecache;
    final ByteBuffer pagecacheBuffer;
    private final AtomicInteger synccounter = new AtomicInteger(0);

    public PageCache(int maxObjects) {
        int alignOffset = 0;
        if(isLinux) {
            pagecacheBuffer = ByteBuffer.allocateDirect(maxObjects * PAGESIZE + PAGESIZE - 1);
            alignOffset = getAlignOffset(pagecacheBuffer);
            logger.trace(Util.delayedFormatString("the offset to align is %d", alignOffset));
        }
        else {
            pagecacheBuffer = ByteBuffer.allocateDirect(maxObjects * PAGESIZE);
        }

        //Create the page cache in memory
        pagecache = new LRUArray<FilePage>(maxObjects);
        //And fill it with empty pages
        for(int i = 0 ; i < maxObjects ; i++ ) {
            pagecache.put(i, new FilePage(pagecacheBuffer, alignOffset, i));
        }

        logger.info(Util.delayedFormatString("created a page cache with %d %d pages, using %d of memory", maxObjects, PAGESIZE, maxObjects * PAGESIZE));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        sync();
    }

    /**
     * Find the page that match the given offset and file. If it's not already in memory, it's loaded 
     * @param file the file where to find data
     * @param offset the offset in the file
     * @return the page that will contains the given offset
     * @throws IOException
     * @throws InterruptedException 
     */
    private FilePage find(File file, long offset) throws IOException, InterruptedException {
        FileInfo fi = getFileInfo(file);

        long offsetPage = offsetPage(offset);
        FilePage page = null;
        page = fi.pagesList.get(offsetPage);
        // Page is not cached
        // we need to free an old one and use it
        if(page == null) {
            fi.lock.readLock().unlock();
            //page is remove from the page cache, but it will be put back
            page = pagecache.removeEldest();
            //We getting an already used page, it needs to be clean before reuse
            if(! page.isEmpty()) {
                logger.trace(Util.delayedFormatString("Flushing page %d, used by file %s at offset %d", page.pageIndex, page.filepath, page.pageIndex));
                final String filepath = page.filepath;
                FileInfo oldFi = files.get(filepath);
                oldFi.lock.writeLock().lockInterruptibly();
                boolean isDirty = page.isDirty();
                //Remove page from page used by this file and clean it
                page.free();
                oldFi.pagesList.remove(page.fileOffset);
                oldFi.lock.writeLock().unlock();
                //Launch a synchronization thread if needed for this file, to keep it on a coherent state on disk
                if(isDirty) {
                    Thread syncthread = new Thread() {
                        @Override
                        public void run() {
                            Map<Long, FilePage> pages = PageCache.this.files.get(filepath).pagesList;
                            syncFilePages(pages);
                            PageCache.this.synccounter.decrementAndGet();
                        }
                    };
                    syncthread.setName("JRDSPageCacheSync-" + new File(filepath).getName() + "-" + synccounter.getAndIncrement());
                    syncthread.setDaemon(true);
                    syncthread.start();
                }
            }
            //Put back, this time assigned to the good file
            fi.lock.writeLock().lockInterruptibly();
            //Check if allocation is still needed
            if(! fi.pagesList.containsKey(offsetPage)) {
                pagecache.put(page.pageIndex, page);
                fi.pagesList.put(offsetPage, page);
            }
            else {
                page = fi.pagesList.get(offsetPage);
            }
            fi.lock.readLock().lockInterruptibly();
            fi.lock.writeLock().unlock();
        }
        // The page gotten in the synchronized section may be an empty one
        // we need to load it, but not in the synchronized section 
        if(page.isEmpty())
            page.load(file, offsetPage);

        logger.trace(Util.delayedFormatString("Loading at offset %d from %s in page %d", offsetPage, page.filepath, page.pageIndex));

        return page;
    }

    public void read(File file, long offset, byte[] buffer) throws IOException, InterruptedException {
        logger.debug(Util.delayedFormatString("Loading %d bytes at offset %d from %s", buffer.length, offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = offsetPage(offset);
        long cacheEnd = offsetPage(offset + buffer.length - 1);
        ReadWriteLock lock = getFileInfo(file).lock;
        while(cacheStart <= cacheEnd) {
            lock.readLock().lockInterruptibly();
            FilePage current = find(file, cacheStart);
            current.read(offset, buffer);
            cacheStart += PAGESIZE;
            lock.readLock().unlock();
        }
    }

    public void write(File file, long offset, byte[] buffer) throws IOException, InterruptedException {
        logger.debug(Util.delayedFormatString("Writing %d bytes at offset %d to %s", buffer.length, offset, file.getCanonicalPath()));
        if(buffer.length == 0)
            return;

        long cacheStart = offsetPage(offset);
        long cacheEnd = offsetPage(offset + buffer.length - 1);
        ReadWriteLock lock = getFileInfo(file).lock;
        while(cacheStart <= cacheEnd) {
            lock.readLock().lockInterruptibly();
            FilePage current = find(file, cacheStart);
            current.write(offset, buffer);
            cacheStart += PAGESIZE;
            lock.readLock().unlock();
        }
    }

    /**
     * Sync all pages from a single file
     * @param pagepointer
     */
    private void syncFilePages(Map<Long, FilePage> pagepointer) {
        FileChannel channel = null;
        for(FilePage page: pagepointer.values()) {
            try {
                channel = page.sync(channel);
            } catch (IOException e) {
                logger.error(Util.delayedFormatString("sync failed for %s:", page.filepath, e));
            }
        }
        if(channel!= null) {
            try {
                channel.force(true);
                channel.close();
            } catch (IOException e) {
                logger.error(Util.delayedFormatString("sync failed for %s: %e", channel, e));
            }
        }
    }

    public void sync() {
        for( FileInfo fi: files.values()) {
            syncFilePages(fi.pagesList);
        }
    }

    static final long offsetPage(long offset) {
        return offset - ( offset %  PAGESIZE );
    }

    private final FileInfo getFileInfo(File file) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        //Locate the pages per file map
        FileInfo fi = files.get(canonicalPath);
        if(fi == null) {
            fi = new FileInfo();
            files.putIfAbsent(canonicalPath, fi);
        }
        return files.get(canonicalPath);
    }
}
