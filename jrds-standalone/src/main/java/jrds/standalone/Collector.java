package jrds.standalone;

import java.io.File;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.starter.Timer;

/**
 * @author Fabrice Bacchella
 *
 */
public class Collector extends CommandStarterImpl {
    static final private Logger logger = LoggerFactory.getLogger(Collector.class);

    String propFile = "jrds.properties";

    public void configure(Properties configuration) {
        logger.debug("Configuration: " + configuration);

        propFile = configuration.getProperty("propertiesFile", propFile);
    }

    public void start(String[] args) {

        PropertiesManager pm = new PropertiesManager(new File(propFile));

        System.getProperties().setProperty("java.awt.headless", "true");
        System.getProperties().putAll(pm);

        HostsList hl = new HostsList(pm);

        logger.debug("Scanning dir");

        for(Timer t: hl.getTimers()) {
            t.collectAll();
        }
    }

    @Override
    public String getName() {
        return "collect";
    }

}
