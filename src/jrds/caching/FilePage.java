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

    private final ByteBuffer page;
    final int pageIndex;
    private boolean dirty;
    private int size;
    String filepath;
    long fileOffset;
    
    /**
     * Used to build an empty page
     */
    public FilePage(ByteBuffer pagecache, int pageIndex) {
        try {
            pagecache.position(pageIndex * PageCache.PAGESIZE);
            this.page = pagecache.slice();
            this.page.limit(0);
            this.pageIndex = pageIndex;
            this.size = 0;
            logger.trace(Util.delayedFormatString("Sliced at %d, capacity: %d", pagecache.position(), page.capacity()));
        } catch (RuntimeException e) {
            logger.error(Util.delayedFormatString("page creation failed at %d", pageIndex));
            throw e;
        }
    }

    public FilePage(int pageIndex) {
        try {

            this.page = ByteBuffer.allocateDirect(PageCache.PAGESIZE);
            this.page.limit(0);
            this.pageIndex = pageIndex;
            this.size = 0;
        } catch (RuntimeException e) {
            logger.error(Util.delayedFormatString("page creation failed at %d", pageIndex));
            throw e;
        }
    }

    public void load(File file,
            long offset) throws IOException {
        this.filepath = file.getCanonicalPath();
        this.fileOffset = PageCache.offsetPage(offset);
        this.page.limit(PageCache.PAGESIZE);
        this.page.position(0);
        FileChannel channel = DirectFileRead(filepath, false);
        this.size = channel.read(page);
        logger.debug(Util.delayedFormatString("Loaded %d bytes at offset %d from %s in page %d", size, fileOffset, filepath, pageIndex));
    }

    public synchronized void sync() throws IOException {
        if(dirty) {
            try {
                logger.debug(Util.delayedFormatString("syncing %d bytes at %d to %s", size, fileOffset, filepath));
                page.position(0);
                page.limit(size);
                FileChannel channel = DirectFileWrite(filepath, false);
                channel.write(page, fileOffset);
                channel.force(true);
                dirty = false;
            } catch (IOException e) {
                logger.error(Util.delayedFormatString("sync failed for %s: %s", filepath, e), e);
                throw e;
            } catch (IllegalArgumentException e) {
                logger.error(Util.delayedFormatString("sync failed for %s: %s, writing %d bytes", filepath, e, size), e);
                throw e;
            } catch (RuntimeException e) {
                logger.error(Util.delayedFormatString("sync failed for %s: %s", filepath, e), e);
                throw e;
            }
        }
    }

    public void read(long offset, byte[] buffer) throws IOException{
        int pageStartPos = (int) Math.max(0, offset - fileOffset);
        int pageEndPos = (int) Math.min((long)PageCache.PAGESIZE, offset + buffer.length - fileOffset);
        int bytesRead = pageEndPos - pageStartPos;
        int bufferOffset = (int) Math.max(0, fileOffset - offset);
        try {
            page.limit(pageEndPos);
            page.position(pageStartPos);
            page.get(buffer, bufferOffset, bytesRead);
        } catch (Exception e) {
            logger.error(Util.delayedFormatString("error getting %d bytes at %d, starting at %d, from %d to %d %d %d", buffer.length, offset, fileOffset, pageStartPos, pageEndPos - 1, bufferOffset, bytesRead));
            logger.error(e, e);
        }
        logger.trace(Util.delayedFormatString("in page %d: read %d bytes at offset %d in file %s: relative to offset %d of file, from %d to %d (%d bytes) in position %d in buffer", pageIndex, buffer.length, offset, filepath, fileOffset, pageStartPos, pageEndPos - 1, bytesRead, bufferOffset));
    }

    public void write(long offset, byte[] buffer) {
        int pageStartPos = (int) Math.max(0, offset - fileOffset);
        int pageEndPos = (int) Math.min((long)PageCache.PAGESIZE, offset + buffer.length - fileOffset);
        int bytesWritten = pageEndPos - pageStartPos;
        int bufferOffset = (int) Math.max(0, fileOffset - offset);
        try {
            page.limit(pageEndPos);
            page.position(pageStartPos);
            page.put(buffer, bufferOffset, bytesWritten);
        } catch (Exception e) {
            logger.error(Util.delayedFormatString("%d %d %d %d", pageStartPos, pageEndPos, bufferOffset, bytesWritten));
            logger.error(e, e);
        }
        size = Math.max(size, pageEndPos);
        logger.debug(Util.delayedFormatString("in page %d: write %d bytes at %d from %s, in page %d of file from %d to %d to %d", pageIndex, bytesWritten, offset, filepath, fileOffset / PageCache.PAGESIZE, pageStartPos, pageEndPos, bufferOffset));

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
        return filepath == null;
    }

    public void free() throws IOException {
        if(filepath != null)
            sync();
        filepath = null;
        page.limit(0);
        size = 0;
    }

    /**
     * @return the dirty
     */
    boolean isDirty() {
        return dirty;
    }
}
