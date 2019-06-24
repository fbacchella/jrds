package jrds.store;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdFileBackendFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Util;
import jrds.factories.ArgFactory;

public class RrdDbStoreFactory extends AbstractStoreFactory<RrdDbStore> {
    private static final Logger logger = LoggerFactory.getLogger(RrdDbStoreFactory.class);
    private RrdBackendFactory backendFactory = null;
    private RrdDbPool instance = null;

    private final AtomicLong waitTime = new AtomicLong(0);
    private final AtomicInteger lockCount = new AtomicInteger(0);
    private boolean usepool = false;
    private int dbPoolSize = 0;

    String backendName = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * jrds.store.AbstractStoreFactory#configureStore(jrds.PropertiesManager)
     */
    @Override
    public void configureStore(PropertiesManager pm, Properties props) {
        super.configureStore(pm, props);

        String backendName = null;
        // Choose and configure the backend
        String rrdbackendClassName = props.getProperty("rrdbackendclass", "");
        if(!"".equals(rrdbackendClassName)) {
            try {
                @SuppressWarnings("unchecked")
                Class<RrdBackendFactory> factoryClass = (Class<RrdBackendFactory>) pm.extensionClassLoader.loadClass(rrdbackendClassName);
                backendFactory = factoryClass.getConstructor().newInstance();
                RrdBackendFactory.setActiveFactories(backendFactory);
                backendName = backendFactory.getName();
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure RrdDbStoreFactory:" + e.getMessage(), e);
            }
        } else {
            backendName = props.getProperty("rrdbackend", "FILE");
            backendFactory = RrdBackendFactory.getFactory(backendName);
        }
        
        RrdBackendFactory.setActiveFactories(backendFactory);

        // Analyze the backend properties
        Map<String, String> backendPropsMap = pm.subKey("rrdbackend");

        if(backendPropsMap.size() > 0) {
            logger.debug("Configuring backend factory {}", backendFactory.getClass());
            for(Map.Entry<String, String> e: backendPropsMap.entrySet()) {
                try {
                    logger.trace("Will set backend end bean '{}' to '{}'", e.getKey(), e.getValue());
                    ArgFactory.beanSetter(backendFactory, e.getKey(), e.getValue());
                } catch (InvocationTargetException e1) {
                    throw new RuntimeException("Failed to configure RrdDbStoreFactory:" + e1.getMessage(), e1);
                }
            }
        }

        dbPoolSize = Util.parseStringNumber(props.getProperty("dbPoolSize"), 10) + pm.numCollectors;
        usepool = pm.parseBoolean(props.getProperty("usepool", "true"));

        logger.debug("Store backend used is {}", backendName);

    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.store.AbstractStoreFactory#start()
     */
    @Override
    public void start() {
        super.start();
        boolean filebasedbackend = backendFactory instanceof RrdFileBackendFactory;
        if(usepool || filebasedbackend) {
            try {
                RrdBackendFactory.setActiveFactories(backendFactory);;
                logger.trace("Store backend set to {}", backendName);
            } catch (IllegalStateException e) {
                logger.warn("Trying to change default backend, a restart is needed");
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure RrdDbStoreFactory:" + e.getMessage(), e);
            }
            try {
                instance = RrdDbPool.getInstance();
                instance.setCapacity(dbPoolSize);
                usepool = true;
            } catch (Exception e) {
                logger.warn("Trying to change rrd pool size, a restart is needed");
            }
        }
    }

    @Override
    public RrdDbStore create(Probe<?, ?> p) {
        return new RrdDbStore(p, this);
    }

    @Override
    public void stop() {
        logger.info("Average wait time: {} ms", waitTime.doubleValue() / lockCount.doubleValue());
    }

    /**
     * Retrieves the RrdDb instance matching a specific RRD datasource name
     * (usually a file name) and using a specified RrdBackendFactory.
     *
     * @param rrdFile Name of the RRD datasource.
     * @return RrdDb instance of the datasource.
     * @throws IOException Thrown in case of I/O error.
     */
    public RrdDb getRrd(String rrdFile) throws IOException {
        Path rrdPath = Paths.get(rrdFile);
        long start = System.currentTimeMillis();
        RrdDb db = RrdDb.getBuilder().setPath(rrdPath.toRealPath(LinkOption.NOFOLLOW_LINKS).normalize().toUri()).setBackendFactory(backendFactory).build();
        long finish = System.currentTimeMillis();
        waitTime.addAndGet(finish - start);
        lockCount.incrementAndGet();
        return db;
    }

    /**
     * @param db
     */
    public void releaseRrd(RrdDb db) {
        try {
            long start = System.currentTimeMillis();
            db.close();
            long finish = System.currentTimeMillis();
            waitTime.addAndGet(finish - start);
            lockCount.incrementAndGet();
        } catch (IOException e) {
            logger.error("Unable to release RrdDb '{}': {}", db.getPath(), e.getMessage());
        }
    }

    /**
     * @param rrdDb
     * @return
     * @throws IOException
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(org.rrd4j.core.RrdDb)
     */
    public int getOpenCount(RrdDb rrdDb) throws IOException {
        return usepool ? instance.getOpenCount(rrdDb) : 0;
    }

    /**
     * @param path
     * @return
     * @throws IOException
     * @see org.rrd4j.core.RrdDbPool#getOpenCount(java.lang.String)
     */
    public int getOpenCount(String path) throws IOException {
        return usepool ? instance.getOpenCount(path) : 0;
    }

    public String[] getOpenFiles() {
        return usepool ? instance.getOpenFiles() : new String[] {};
    }

}
