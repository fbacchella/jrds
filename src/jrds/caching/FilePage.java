package jrds.caching;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import jrds.Util;

import org.apache.log4j.Logger;

public class FilePage {
    static final private Logger logger = Logger.getLogger(FilePage.class);

    private native static void prepare_fd(String filename, FileDescriptor fdobj, boolean readOnly);

    private static final FileChannel DirectFileRead(String path, boolean readOnly) throws IOException {
        FileDescriptor fd = new FileDescriptor();
        prepare_fd(path, fd, readOnly);
        return new FileInputStream(fd).getChannel();
    }
    
    private static final FileChannel DirectFileWrite(String path, boolean readOnly) throws IOException {
        FileDescriptor fd = new FileDescriptor();
        prepare_fd(path, fd, readOnly);
        return new FileOutputStream(fd).getChannel();
    }

    static {
        System.load("/var/jrds/WEB-INF/lib/libdirect.dylib");
    }

    static public final FilePage EMPTY = new FilePage() {
        @Override
        public synchronized void sync() throws IOException {
        }
        @Override
        public void read(long offset, byte[] buffer) throws IOException {
        }
        @Override
        public void write(long offset, byte[] buffer) {
        }
        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    private final ByteBuffer page;
    private boolean dirty;
    final String filepath;
    private final long fileOffset;
    final int pageIndex;
    private int size;

    /**
     * Used to build an empty page
     */
    private FilePage() {
        page = null;
        filepath = null;
        fileOffset = 0;
        pageIndex = 0;
        this.size = 0;
    }
    
     public FilePage(ByteBuffer pagecache, int pageIndex, File file,
            long offset) throws IOException {
        pagecache.position(pageIndex * PageCache.PAGESIZE);
        this.page = pagecache.slice();
        this.page.limit(PageCache.PAGESIZE);
        this.filepath = file.getCanonicalPath();
        this.fileOffset = (long) (Math.floor( offset /  PageCache.PAGESIZE) * PageCache.PAGESIZE);
        FileChannel channel = DirectFileRead(filepath, false);
        this.size = channel.read(page);
        this.pageIndex = pageIndex;
        logger.debug(Util.delayedFormatString("Loaded %d bytes at offset %d from %s", size, fileOffset, file.getCanonicalPath()));
    }

    public synchronized void sync() throws IOException {
        if(dirty) {
            try {
                FileChannel channel = DirectFileWrite(filepath, false);
                page.position(0);
                page.limit(size + 1);
                channel.write(page, fileOffset);
                channel.force(true);
                dirty = false;
            } catch (IOException e) {
                logger.error(Util.delayedFormatString("sync failed for %s", filepath), e);
                throw e;
            }
        }
    }

    public void read(long offset, byte[] buffer) throws IOException{
        //The offset within the page
        int pageStartPos = (int) Math.max(0, offset - fileOffset);
        int pageEndPos = (int) Math.min((long)PageCache.PAGESIZE - 1, offset + buffer.length - 1 - fileOffset);
        int bytesRead = pageEndPos - pageStartPos + 1;
        int bufferOffset = (int) Math.max(0, fileOffset - offset);
        try {
            page.limit(pageEndPos + 1);
            page.position(pageStartPos);
            page.get(buffer, bufferOffset, bytesRead);
        } catch (Exception e) {
            logger.error(Util.delayedFormatString("getting %d bytes at %d, starting at %d, from %d to %d %d %d", buffer.length, offset, fileOffset, pageStartPos, pageEndPos, bufferOffset, bytesRead));
            logger.error(e, e);
        }
        logger.debug(Util.delayedFormatString("read %d bytes from %s", bytesRead, filepath));
    }

    public void write(long offset, byte[] buffer) {

        //The offset within the page
        int pageStartPos = (int) Math.max(0, offset - fileOffset);
        int pageEndPos = (int) Math.min((long)PageCache.PAGESIZE - 1, offset + buffer.length - 1 - fileOffset);
        int bytesWritten = pageEndPos - pageStartPos + 1;
        int bufferOffset = (int) Math.max(0, fileOffset - offset);
        try {
            page.limit(pageEndPos + 1);
            page.position(pageStartPos);
            page.put(buffer, bufferOffset, bytesWritten);
        } catch (Exception e) {
            logger.error(Util.delayedFormatString("%d %d %d %d", pageStartPos, pageEndPos, bufferOffset, bytesWritten));
            logger.error(e, e);
        }
        size = Math.max(size, pageEndPos);
        logger.debug(Util.delayedFormatString("%d %d maps to %d %d", offset, buffer.length, pageStartPos, bytesWritten));
        logger.debug(Util.delayedFormatString("write %d bytes to %s", bytesWritten, filepath));

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

    public boolean isEmpty() {
        return false;
    }
}
