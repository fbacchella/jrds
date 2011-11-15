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
import org.rrd4j.core.RrdBackendMeta;

/**
 * The factory for a RrdCachedFileBackend. It preloads the directIO JNI wrapper library by expecting it to be in the 
 * same directory as the class' jar. An helper method loadDirect() can also be used.
 * 
 * @author Fabrice Bacchella
 *
 */
@RrdBackendMeta("CACHEDFILE")
public class RrdCachedFileBackendFactory extends RrdFileBackendFactory {
    static final private Logger logger = Logger.getLogger(RrdCachedFileBackendFactory.class);

    static private boolean jni_loaded = false;
    private PageCache pagecache = null;
    private Timer syncTimer = null;
    private int numpages;
    private int syncPeriod;

    /**
     * An help class that can be used to load the directIO JNI wrapper library.
     * @param path
     * @throws IOException
     */
    static final public void loadDirect(File path) throws IOException {
        String directlib = System.mapLibraryName("direct");
        File directlibfile = new File(path,directlib);
        if(directlibfile.exists()) {
            System.load(directlibfile.getCanonicalPath());
            jni_loaded = true;
        }
    }

    /**
     * Defines the embedded page cache size. Its pages are 4096 bytes in size
     * @param maxObjects the number of page
     */
    public void setPageCache(int numpages) {
        this.numpages = numpages;
    }

    /**
     * Defines the synchronization period for the page scanner
     * @param syncPeriod
     */
    public void setSyncPeriod(int syncPeriod) {
        this.syncPeriod = syncPeriod;
    }

    /**
     * Creates RrdFileBackend object for the given file path.
     * @param path File path
     * @param readOnly True, if the file should be accessed in read/only mode.
     * False otherwise.
     * @throws IOException Thrown in case of I/O error.
     */
    public RrdBackend doOpen(String path, boolean readOnly) throws IOException {
        return new RrdCachedFileBackend(path, readOnly, pagecache);
    }

    /**
     * Synchronizes all the dirty page to the disk
     */
    @Override
    public void doSync() {
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

    @Override
    protected boolean startBackend() {
        if( !jni_loaded ) {
            URL classURL = StoreOpener.class.getClassLoader().getResource("jrds/caching/RrdCachedFileBackendFactory.class");
            String classfile = classURL.getPath().replaceAll("!.*", "").replaceAll("file:", "");
            File homeFile = new File(classfile).getParentFile();
            try {
                loadDirect(homeFile);
                jni_loaded = true;
            } catch (IOException e) {
                logger.error(Util.delayedFormatString("direct lib not loaded: %s", e));
                return false;
            } catch (UnsatisfiedLinkError e) {
                logger.error(Util.delayedFormatString("direct lib not loaded: %s", e));
                return false;
            }
        }
        //Allocate the pages
        pagecache = new PageCache(numpages);
        TimerTask syncTask = new TimerTask() {
            public void run() {
                PageCache pagecache = RrdCachedFileBackendFactory.this.pagecache;
                if(pagecache != null)
                    pagecache.sync();
            }
        };
        syncTimer = new Timer("RrdCachedFileBackendFactory-sync", true);
        syncTimer.schedule(syncTask, syncPeriod * 1000L, syncPeriod * 1000L);
        logger.info(Util.delayedFormatString("created a page cache with %d %d pages, using %d bytes qof memory", numpages, PageCache.PAGESIZE, numpages * PageCache.PAGESIZE));
        return true;
    }

    @Override
    protected boolean stopBackend() {
        pagecache.sync();
        pagecache = null;
        syncTimer.cancel();
        syncTimer = null;
        return true;
    }

}