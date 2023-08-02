package jrds;

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

    public static synchronized Configuration configure(Properties p) {
        conf = new Configuration(p);
        conf.start();
        return conf;
    }

    public static synchronized Configuration switchConf(Properties p) {
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

    public static synchronized Configuration get() {
        return conf;
    }

    public static synchronized void stopConf() {
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
        if (propertiesManager.readonly) {
            return;
        }
        // Add a shutdown hook, the shutdown signal might be send before the
        // listener is stopped
        shutDownHook = new Thread(hostsList::stopCollect, "Collect-Shutdown");
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
