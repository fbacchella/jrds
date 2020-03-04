package jrds;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    static private final Logger logger = LoggerFactory.getLogger(Configuration.class);

    static private Configuration conf;

    private final PropertiesManager propertiesManager = new PropertiesManager();
    private final HostsList hostsList;
    private Thread shutDownHook;

    public static final synchronized Configuration configure(Properties p) {
        conf = new Configuration(p);
        conf.start();
        return conf;
    }

    public static final synchronized Configuration switchConf(Properties p) {
        Configuration oldConfig = conf;
        Configuration newConfig = new Configuration(p);
        oldConfig.stop();
        newConfig.start();
        conf = newConfig;
        // Avoid a memory leak in perm gen
        java.beans.Introspector.flushCaches();
        logger.info("Configuration rescaned");
        return conf;
    }

    public static final synchronized Configuration get() {
        return conf;
    }

    public static final synchronized void stopConf() {
        conf.stop();
    }

    private Configuration(Properties p) {
        propertiesManager.join(p);
        propertiesManager.importSystemProps();
        propertiesManager.update();

        hostsList = new HostsList(propertiesManager);
    }

    private void start() {
        // If in read-only mode, no scheduler
        if(propertiesManager.readonly)
            return;
        // Add a shutdown hook, the shutdown signal might be send before the
        // listener is stopped
        shutDownHook = new Thread("Collect-Shutdown") {
            @Override
            public void run() {
                hostsList.stopCollect();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutDownHook);
        hostsList.startTimers();
    }

    private void stop() {
        hostsList.stopCollect();
        try {
            Optional.ofNullable(shutDownHook).ifPresent(s -> Runtime.getRuntime().removeShutdownHook(s));
        } catch (Exception ex) {
            // We don't care if it failed, just tried
        }
        try {
            propertiesManager.close();
        } catch (IOException e) {
            logger.error("Failed to close old classloader: %s", e.getMessage());
        }
    }

    /**
     * @return the hostsList
     */
    final public HostsList getHostsList() {
        return hostsList;
    }

    /**
     * @return the propertiesManager
     */
    final public PropertiesManager getPropertiesManager() {
        return propertiesManager;
    }

}
