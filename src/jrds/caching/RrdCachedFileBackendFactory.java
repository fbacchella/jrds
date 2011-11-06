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
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdFileBackendFactory;

/**
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

    static final public void loadDirect(File path) throws IOException {
        String directlib = System.mapLibraryName("direct");
        File directlibfile = new File(path,directlib);
        if(directlibfile.exists())
            System.load(directlibfile.getCanonicalPath());
    }

    public void setPageCache(int maxObjects) {
        if(pagecache != null)
            pagecache.sync();
        pagecache = new PageCache(maxObjects);
    }

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
     * @param lockMode One of the following constants: {@link RrdDb#NO_LOCKS},
     * {@link RrdDb#EXCEPTION_IF_LOCKED} or {@link RrdDb#WAIT_IF_LOCKED}.
     * @return RrdFileBackend object which handles all I/O operations for the given file path
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

    public void sync() {
        pagecache.sync();
    }

    /* (non-Javadoc)
     * @see org.rrd4j.core.RrdRandomAccessFileBackendFactory#shouldValidateHeader(java.lang.String)
     */
    @Override
    protected boolean shouldValidateHeader(String path) throws IOException {
        return true;
    }

}