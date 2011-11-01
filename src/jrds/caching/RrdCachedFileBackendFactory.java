package jrds.caching;

import java.io.File;
import java.io.IOException;

import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;

/**
 * @author Fabrice Bacchella
 *
 */
public class RrdCachedFileBackendFactory extends RrdRandomAccessFileBackendFactory {
    /** factory name, "CACHEDFILE" */
    public static final String NAME = "CACHEDFILE";

    private static PageCache pagecache = null;
    
    public static void setPageCache(int maxObjects, int syncPeriod) {
        if(pagecache != null)
            pagecache.sync();
        pagecache = new PageCache(maxObjects, syncPeriod);
    }
            
    /**
     * Creates RrdFileBackend object for the given file path.
     * @param path File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     * False otherwise.
     * @param lockMode One of the following constants: {@link RrdDb#NO_LOCKS},
     * {@link RrdDb#EXCEPTION_IF_LOCKED} or {@link RrdDb#WAIT_IF_LOCKED}.
     * @return RrdFileBackend object which handles all I/O operations for the given file path
     * @throws IOException Thrown in case of I/O error.
     */
    public RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdCachedFileBackend(path, readOnly, pagecache);
    }

    /**
     * Method to determine if a file with the given path already exists.
     * @param path File path
     * @return True, if such file exists, false otherwise.
     */
    public boolean exists(String path) {
        return new File(path).exists();
    }

    /**
     * Returns the name of this factory.
     * @return Factory name (equals to string "CACHEDFILE")
     */
    public String getName() {
        return NAME;
    }
    
    public void sync() {
        pagecache.sync();
    }

}