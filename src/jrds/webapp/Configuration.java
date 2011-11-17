package jrds.webapp;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;

import org.apache.log4j.Logger;

public class Configuration {
    static private final Logger logger = Logger.getLogger(Configuration.class);

    static final private AtomicInteger generation = new AtomicInteger(0);

    private PropertiesManager propertiesManager = new PropertiesManager();
    private HostsList hostsList = null;
    private Timer collectTimer;
    public int thisgeneration = generation.incrementAndGet();
    TimerTask collector;
    Thread shutDownHook;

    /**
     * A constructor used to simplify tests
     * @param ctxt
     */
    Configuration(Properties ctxt) {
        propertiesManager.join(ctxt);
        finishConfig();
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
        finishConfig();
    }

    private void finishConfig() {

        propertiesManager.importSystemProps();
        propertiesManager.update();

        StoreOpener.prepare(propertiesManager.rrdbackend, propertiesManager.dbPoolSize);

        hostsList = new HostsList(propertiesManager);
    }

    public void start() {
        //If in read-only mode, no scheduler
        if(propertiesManager.readonly)
            return;
        collectTimer = new Timer("jrds-main-timer", true);
        collector = new TimerTask () {
            public void run() {
                try {
                    hostsList.collectAll();
                } catch (RuntimeException e) {
                    logger.fatal("A fatal error occured during collect: ",e);
                }
            }
        };
        collectTimer.schedule(collector, 5000L, propertiesManager.step * 1000L);
        // Add a shutdown hook, the shutdown signal might be send before the listener is stopped
        shutDownHook = new Thread("Collect-Shutdown") {
            @Override
            public void run() {
                if(hostsList != null)
                    hostsList.finished();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    public void stop() {
        if(collector != null)
            collector.cancel();
        hostsList.finished();
        Thread.yield();
        //We don't care if it failed, just try
        try {
            if(shutDownHook != null)
                Runtime.getRuntime().removeShutdownHook(shutDownHook);
        } catch (Exception e1) {
        }
        try {
            hostsList.lockCollect();
        } catch (InterruptedException e) {
        }
        hostsList.getRenderer().finish();
        if(collectTimer != null)
            collectTimer.cancel();
        collectTimer = null;
        hostsList.releaseCollect();
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
    public HostsList getHostsList() {
        return hostsList;
    }

    /**
     * @return the propertiesManager
     */
    public PropertiesManager getPropertiesManager() {
        return propertiesManager;
    }

}
