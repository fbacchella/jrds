/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;
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
	private static final Logger logger = Logger.getLogger(Collector.class);
	private static final PropertiesManager pm = PropertiesManager.getInstance();
	public static void main(String[] args) throws Exception {
		pm.join(new File("jrds.properties"));
		pm.update();
		
		System.getProperties().setProperty("java.awt.headless","true");
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);
		
		final HostsList hl = HostsList.fill(new File(pm.configfilepath));

		logger.setLevel(Level.ERROR);
		Logger.getRootLogger().setLevel(Level.ERROR);
		Logger.getLogger("jrds").setLevel(Level.ALL);
		logger.info("jrds' collector started");
		Logger.getLogger("org.snmp4j").setLevel(Level.ERROR);
		SnmpRequester.start();
		for(int i = 0; i< 5 ; i++) {
			hl.collectAll();
			System.gc();
		}
		SnmpRequester.stop();
		StoreOpener.stop();
		System.gc();
	}
	
}

