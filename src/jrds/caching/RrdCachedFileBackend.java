package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import jrds.Util;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdBackend;

/** 
 * JRobin backend which is used to store RRD data to ordinary disk files 
 * by using fast java.nio.* package ehanced with caching functionnalities.
 * @author Fabrice Bacchella
 */
public class RrdCachedFileBackend extends RrdBackend {
    static final private Logger logger = Logger.getLogger(RrdCachedFileBackend.class);

    private final boolean readOnly;
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
        super(path);
        this.readOnly = readOnly;
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
            logger.debug(Util.delayedFormatString("Loading page %d from %s", offset, file.getCanonicalPath()));
            pagecache.read(file, offset, b);
        }
    }

    /** 
     * Closes the underlying RRD file. 
     * @throws IOException Thrown in case of I/O error 
     */
    public void close() throws IOException {

        super.close(); // calls sync() eventually
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
//        file.
//        if(channel.size() < length)
//            channel.truncate(length);
//        else {
//            channel.position(length);
//            channel.write(ByteBuffer.allocate(1));
//        }
    }

}