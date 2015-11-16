package jrds;

import java.util.Properties;

import jrds.starter.Timer;
import jrds.store.StoreFactory;

import org.apache.log4j.Logger;

public class Configuration {
    static private final Logger logger = Logger.getLogger(Configuration.class);

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
        //Avoid a memory leak in perm gen
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
        //If in read-only mode, no scheduler
        if(propertiesManager.readonly)
            return;
        // Add a shutdown hook, the shutdown signal might be send before the listener is stopped
        shutDownHook = new Thread("Collect-Shutdown") {
            @Override
            public void run() {
                hostsList.stop();
                if(hostsList.getRenderer() != null)
                    hostsList.getRenderer().finish();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutDownHook);
        hostsList.startTimers();
    }

    private void stop() {
        hostsList.stop();
        Thread.yield();
        //We don't care if it failed, just try
        try {
            if(shutDownHook != null)
                Runtime.getRuntime().removeShutdownHook(shutDownHook);
        } catch (Exception e1) {
        }
        //Everything is stopped, wait for collect termination
        try {
            for(Timer t: hostsList.getTimers()) {
                t.lockCollect();
            }
            for(Timer t: hostsList.getTimers()) {
                //Release it, it will not restart
                t.releaseCollect();
            }
        } catch (InterruptedException e) {
        }
        for(StoreFactory sf: propertiesManager.stores.values()) {
            sf.stop();
        }
        if(hostsList.getRenderer() != null) {
            hostsList.getRenderer().finish();            
        }
        propertiesManager.defaultStore.stop();
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
