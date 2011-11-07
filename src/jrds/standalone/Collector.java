/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.util.Properties;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Collector extends CommandStarterImpl {
	static final private Logger logger = Logger.getLogger(Collector.class);

	String propFile = "jrds.properties";

	public void configure(Properties configuration) {
		logger.debug("Configuration: " + configuration);
		
		propFile =  configuration.getProperty("propertiesFile", propFile);
	}

	public void start(String[] args) throws Exception {

		PropertiesManager pm = new PropertiesManager(new File(propFile));
		jrds.JrdsLoggerConfiguration.configure(pm);

		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm);
		StoreOpener.prepare(pm.dbPoolSize, pm.timeout, pm.rrdbackend);

		HostsList hl = new HostsList(pm);

		logger.debug("Scanning dir");

		for(int i = 0; i< 1 ; i++) {
			hl.collectAll();
			System.gc();
			//Thread.sleep(10 * 1000L);
		}
		StoreOpener.stop();
	}

}

