package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedByInterruptException;

import jrds.Util;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdFileBackend;

/** 
 * JRobin backend which is used to store RRD data to ordinary disk files 
 * by using fast java.nio.* package ehanced with caching functionnalities.
 * @author Fabrice Bacchella
 */
public class RrdCachedFileBackend extends RrdFileBackend {
    static final private Logger logger = Logger.getLogger(RrdCachedFileBackend.class);

    private final File file;
    private final PageCache pagecache;

    /**
     * Creates RrdFileBackend object for the given file path, backed by RandomAccessFile object.
     *
     * @param path     Path to a file
     * @param readOnly True, if file should be open in a read-only mode. False otherwise
     * @throws IOException Thrown in case of I/O error
     */
    protected RrdCachedFileBackend(String path, boolean readOnly, PageCache pagecache) throws IOException {
        super(path, readOnly);
        this.file = new File(path);
        this.pagecache = pagecache;
    }

    /**
     * Writes bytes to the underlying RRD file on the disk
     * @param offset Starting file offset
     * @param b Bytes to be written.
     * @throws IOException Thrown in case of I/O error
     */
    public void write(long offset, byte[] b) throws IOException {
        if( Thread.currentThread().isInterrupted()) {
            close();
            throw new ClosedByInterruptException();
        }
        else {
            logger.trace(Util.delayedFormatString("Writing %d bytes at %d to %s", b.length, offset, file.getCanonicalPath()));
            pagecache.write(file, offset, b);
        }
    }

    /**
     * Reads a number of bytes from the RRD file on the disk
     * @param offset Starting file offset
     * @param b Buffer which receives bytes read from the file.
     * @throws IOException Thrown in case of I/O error.
     */
    public void read(long offset, byte[] b) throws IOException {
        if( Thread.currentThread().isInterrupted()) {
            close();
            throw new ClosedByInterruptException();
        }
        else {
            logger.trace(Util.delayedFormatString("Loading %d bytes at %d from %s", b.length, offset, file.getCanonicalPath()));
            pagecache.read(file, offset, b);
        }
        if(logger.isDebugEnabled() && b.length == 4) {
            assert b.length == 4 : "Invalid number of bytes for integer conversion";
            int val =  ((b[0] << 24) & 0xFF000000) + ((b[1] << 16) & 0x00FF0000) +
                    ((b[2] << 8) & 0x0000FF00) + ((b[3] << 0) & 0x000000FF);
            logger.debug(Util.delayedFormatString("int value read: %d from %d %d %d %d", val, b[0], b[1], b[2], b[3]));
        }
    }

    /** 
     * Closes the underlying RRD file. 
     * @throws IOException Thrown in case of I/O error 
     */
    public void close() throws IOException {
    }

    /**
     * Returns RRD file length.
     *
     * @return File length.
     * @throws IOException Thrown in case of I/O error.
     */
    public long getLength() throws IOException {
        return file.length();
    }

    /**
     * Sets length of the underlying RRD file. This method is called only once, immediately
     * after a new RRD file gets created.
     *
     * @param length Length of the RRD file
     * @throws IOException Thrown in case of I/O error.
     */
    protected void setLength(long length) throws IOException {
        RandomAccessFile fd = new RandomAccessFile(file, "rw");
        fd.setLength(length);
    }

}