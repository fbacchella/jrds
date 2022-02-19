package jrds.standalone;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.HostInfo;
import jrds.HostsList;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.store.StoreFactory;

public class Updater {
    static final private Logger logger = LoggerFactory.getLogger(Updater.class);

    public static void main(String[] args) {

        PropertiesManager pm = new PropertiesManager(new File("jrds.properties"));
        pm.configureStores();

        System.getProperties().setProperty("java.awt.headless", "true");
        System.getProperties().putAll(pm);
        HostsList hl = new HostsList(pm);

        ExecutorService tpool = Executors.newFixedThreadPool(3);

        for(HostInfo host: hl.getHosts()) {
            for(final Probe<?, ?> p: host.getProbes()) {
                final Runnable runUpgrade = new Runnable() {
                    private Probe<?, ?> lp = p;

                    public void run() {
                        lp.checkStore();
                    }
                };
                try {
                    tpool.execute(runUpgrade);
                } catch (RejectedExecutionException ex) {
                    logger.debug("collector thread dropped for probe " + p.getName());
                }
            }
        }
        tpool.shutdown();
        try {
            tpool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.info("Collect interrupted");
            Thread.currentThread().interrupt();
        }
        for(StoreFactory sf: pm.stores.values()) {
            sf.stop();
        }
        pm.defaultStore.stop();
    }

}
