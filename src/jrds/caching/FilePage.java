package jrds.caching;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FilePage {
    private final ByteBuffer page;
    private boolean dirty;

    final FileChannel file;
    final long fileOffset;

    public FilePage(ByteBuffer pagecache, int pageIndex, FileChannel file,
            long offset) throws IOException {
        super();
        pagecache.position(pageIndex * PageCache.PAGESIZE);
        this.page = pagecache.slice();
        this.page.limit(PageCache.PAGESIZE);
        this.file = file;
        this.fileOffset = (long) (Math.floor( offset /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        file.read(page);
    }

    public synchronized void sync() throws IOException {
        if(dirty) {
            file.write(page, fileOffset);
            dirty = false;
        }
    }

    public void read(byte[] buffer, long offset) throws IOException{
        long offsetPageStart = (long) (Math.floor( offset /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        long offsetPageEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        if(fileOffset < offsetPageStart ||  fileOffset > offsetPageEnd )
            return;
        
        //The offset within the page
        int pageOffset = (int) Math.max(0, offset - fileOffset);
        //The number of bytes to effectively read
        int toRead = (int) Math.min((long)PageCache.PAGESIZE, buffer.length - (pageOffset + offsetPageStart));
        page.position(pageOffset);
        page.get(buffer, (int) (offsetPageStart - fileOffset), toRead);
    }

    public void write(byte[] buffer, int offset, int length) {
        page.put(buffer, offset, Math.min(length, PageCache.PAGESIZE));
        dirty = true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        sync();
    }
}
