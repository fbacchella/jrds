package jrds.caching;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import jrds.StoreOpener;
import jrds.Util;

import org.apache.log4j.Logger;
import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdFileBackendFactory;

/**
 * The factory for a RrdCachedFileBackend. It preloads the directIO JNI wrapper library by expecting it to be in the 
 * same directory as the class' jar. An helper method loadDirect() can also be used.
 * 
 * @author Fabrice Bacchella
 *
 */
public class RrdCachedFileBackendFactory extends RrdFileBackendFactory {
    static final private Logger logger = Logger.getLogger(RrdCachedFileBackendFactory.class);

    /** factory name, "CACHEDFILE" */
    public static final String NAME = "CACHEDFILE";

    private PageCache pagecache = null;
    private Timer syncTimer = null;

    static {
        URL classURL = StoreOpener.class.getClassLoader().getResource("jrds/caching/RrdCachedFileBackendFactory.class");
        String classfile = classURL.getPath().replaceAll("!.*", "").replaceAll("file:", "");
        File homeFile = new File(classfile).getParentFile();
        try {
            loadDirect(homeFile);
        } catch (IOException e) {
            logger.error(Util.delayedFormatString("direct lib not loaded: %s", e));
        } catch (UnsatisfiedLinkError e) {
            logger.error(Util.delayedFormatString("direct lib not loaded: %s", e));
        }
    }

    /**
     * An help class that can be used to load the directIO JNI wrapper library.
     * @param path
     * @throws IOException
     */
    static final public void loadDirect(File path) throws IOException {
        String directlib = System.mapLibraryName("direct");
        File directlibfile = new File(path,directlib);
        if(directlibfile.exists())
            System.load(directlibfile.getCanonicalPath());
    }

    /**
     * Defines the embedded page cache size. Its pages are 4096 bytes in size
     * @param maxObjects the number of page
     */
    public void setPageCache(int maxObjects) {
        if(pagecache != null)
            pagecache.sync();
        pagecache = new PageCache(maxObjects);
    }

    /**
     * Defines the synchronization period for the page scanner
     * @param syncPeriod
     */
    public void setSyncPeriod(int syncPeriod) {
        TimerTask syncTask = new TimerTask() {
            public void run() {
                PageCache pagecache = RrdCachedFileBackendFactory.this.pagecache;
                if(pagecache !=null)
                    pagecache.sync();
            }
        };
        if(syncTimer == null)
            syncTimer = new Timer(true);
        syncTimer.schedule(syncTask, syncPeriod * 1000L, syncPeriod * 1000L);

    }

    /**
     * Creates RrdFileBackend object for the given file path.
     * @param path File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     * False otherwise.
     * @throws IOException Thrown in case of I/O error.
     */
    public RrdBackend open(String path, boolean readOnly) throws IOException {
        return new RrdCachedFileBackend(path, readOnly, pagecache);
    }

    /**
     * Returns the name of this factory.
     * @return Factory name (equals to string "CACHEDFILE")
     */
    public String getName() {
        return NAME;
    }

    /**
     * Synchronizes all the dirty page to the disk
     */
    public void sync() {
        pagecache.sync();
    }

    /**
     * Determines if the header should be validated.
     *
     * @param path Storage path
     * @return True, if the header should be validated for this factory
     * @throws IOException if header validation fails
     */
    @Override
    protected boolean shouldValidateHeader(String path) throws IOException {
        return true;
    }

}