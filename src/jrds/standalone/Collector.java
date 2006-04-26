/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;

import jrds.DescFactory;
import jrds.HostsList;
import jrds.JrdsLogger;
import jrds.PropertiesManager;
import jrds.StoreOpener;
import jrds.log.JrdsLoggerFactory;
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Collector {
	public static final int GRAPH_RESOLUTION = 300; // seconds
	private static final PropertiesManager pm = PropertiesManager.getInstance();
	static {
		JrdsLogger.setFileLogger(pm.logfile);
	}
	static final private Logger logger = JrdsLoggerFactory.getLogger(Collector.class);

	public static void main(String[] args) throws Exception {
		pm.join(new File("jrds.properties"));
		pm.update();
		
		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm.getProperties());
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);
		
		DescFactory.init();

		final HostsList hl = HostsList.fill(new File(pm.configfilepath));

		logger.setLevel(Level.ERROR);
		Logger.getRootLogger().setLevel(Level.ERROR);
		Logger.getLogger("jrds").setLevel(Level.ALL);
		logger.info("jrds' collector started");
		Logger.getLogger("org.snmp4j").setLevel(Level.ERROR);
		System.getProperties().setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.digester.Digester","debug");
		System.getProperties().setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
		Logger.getLogger("org.apache.commons.digester").setLevel(Level.DEBUG);
		SnmpRequester.start();
		for(int i = 0; i< 1 ; i++) {
			hl.collectAll();
			System.gc();
		}
		SnmpRequester.stop();
		StoreOpener.stop();
		System.gc();
	}
	
}

