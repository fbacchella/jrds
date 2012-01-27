package jrds.webapp;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;
import jrds.starter.Timer;

import org.apache.log4j.Logger;

public class Configuration {
    static private final Logger logger = Logger.getLogger(Configuration.class);

    static final private AtomicInteger generation = new AtomicInteger(0);

    private final PropertiesManager propertiesManager = new PropertiesManager();
    private final HostsList hostsList;
    public final int thisgeneration = generation.incrementAndGet();
    Thread shutDownHook;

    /**
     * A constructor used to simplify tests
     * @param ctxt
     */
    Configuration(Properties ctxt) {
        propertiesManager.join(ctxt);
        hostsList = finishConfig();
    }

    @SuppressWarnings("unchecked")
    public Configuration(ServletContext ctxt) {
        if(logger.isTraceEnabled()) {
            dumpConfiguration(ctxt);
        }

        InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
        if(propStream != null) {
            propertiesManager.join(propStream);
        }

        for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getInitParameterNames())) {
            String value = ctxt.getInitParameter(attr);
            if(value != null)
                propertiesManager.setProperty(attr, value);
        }

        String localPropFile = ctxt.getInitParameter("propertiesFile");
        if(localPropFile != null)
            propertiesManager.join(new File(localPropFile));
        hostsList = finishConfig();
    }

    private HostsList finishConfig() {
        propertiesManager.importSystemProps();
        propertiesManager.update();

        StoreOpener.prepare(propertiesManager.rrdbackend, propertiesManager.dbPoolSize);

        return new HostsList(propertiesManager);
    }

    public void start() {
        //If in read-only mode, no scheduler
        if(propertiesManager.readonly)
            return;
        // Add a shutdown hook, the shutdown signal might be send before the listener is stopped
        shutDownHook = new Thread("Collect-Shutdown") {
            @Override
            public void run() {
                hostsList.finished();
                hostsList.getRenderer().finish();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    public void stop() {
        hostsList.finished();
        Thread.yield();
        //We don't care if it failed, just try
        try {
            if(shutDownHook != null)
                Runtime.getRuntime().removeShutdownHook(shutDownHook);
        } catch (Exception e1) {
        }
        hostsList.getRenderer().finish();
        //Everything is stopped, wait for collect termination
        try {
            for(Timer t: hostsList.getTimers()) {
                t.lockCollect();
                //Release it, it will not restart
                t.releaseCollect();
            }
        } catch (InterruptedException e) {
        }
    }

    @SuppressWarnings("unchecked")
    private void dumpConfiguration(ServletContext ctxt) {
        logger.trace("Dumping attributes");
        for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getAttributeNames())) {
            Object o = ctxt.getAttribute(attr);
            logger.trace(attr + " = (" + o.getClass().getName() + ") " + o);
        }
        logger.trace("Dumping init parameters");
        for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getInitParameterNames())) {
            String o = ctxt.getInitParameter(attr);
            logger.trace(attr + " = " + o);
        }
        logger.trace("Dumping system properties");
        Properties p = System.getProperties();
        for(String attr: jrds.Util.iterate((Enumeration<String>)p.propertyNames())) {
            Object o = p.getProperty(attr);
            logger.trace(attr + " = " + o);
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
