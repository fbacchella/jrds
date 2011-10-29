package jrds.caching;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import jrds.Util;

public class FilePage {
    static final private Logger logger = Logger.getLogger(FilePage.class);

    private native static void prepare_fd(String filename, FileDescriptor fdobj, boolean readOnly);

    private static final FileChannel DirectFile(File direct, boolean readOnly) throws IOException {
        FileDescriptor fd = new FileDescriptor();
        prepare_fd(direct.getCanonicalPath(), fd, readOnly);
        return new FileInputStream(fd).getChannel();
    }

    private final ByteBuffer page;
    private boolean dirty;
    final String filepath;
    private final long fileOffset;

    public FilePage(ByteBuffer pagecache, int pageIndex, File file,
            long offset) throws IOException {
        logger.debug(Util.delayedFormatString("Loading page %d from %s", offset, file.getCanonicalPath()));
        pagecache.position(pageIndex * PageCache.PAGESIZE);
        this.page = pagecache.slice();
        this.page.limit(PageCache.PAGESIZE);
        this.filepath = file.getCanonicalPath();
        this.fileOffset = (long) (Math.floor( offset /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        FileChannel channel = DirectFile(file, false);
        channel.read(page);
    }

    public synchronized void sync() throws IOException {
        if(dirty) {
            FileChannel channel = DirectFile(new File(filepath), false);
            channel.write(page, fileOffset);
            dirty = false;
        }
    }

    public void read(long offset, byte[] buffer) throws IOException{
        long offsetPageStart = (long) (Math.floor( offset /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        long offsetPageEnd = (long) (Math.ceil( (offset + buffer.length - 1)  /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        if(fileOffset < offsetPageStart ||  fileOffset > offsetPageEnd )
            return;

        //The offset within the page
        int pageOffset = (int) Math.max(0, offset - fileOffset);
        int pageEnd = Math.min(PageCache.PAGESIZE, pageOffset + buffer.length - 1);
        int bytesRead = pageEnd - pageOffset + 1;
        int bufferOffset = (int) Math.max(0, fileOffset - offset);
        page.position(pageOffset);
        page.get(buffer, bufferOffset, bytesRead);
        logger.debug(Util.delayedFormatString("read %d bytes from %s", bytesRead, filepath));
    }

    public void write(long offset, byte[] buffer) {
        page.put(buffer, (int) offset, Math.min(0, PageCache.PAGESIZE));
        dirty = true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        sync();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (Long.toString(fileOffset) + "@" + filepath).hashCode() ;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
        if(arg0.getClass() != this.getClass())
            return false;
        FilePage compared = (FilePage) arg0;
        if(! compared.filepath.equals(this.filepath))
            return false;
        return compared.fileOffset == this.fileOffset;
    }


}
