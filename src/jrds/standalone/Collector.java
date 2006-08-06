/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.lang.reflect.Method;

import jrds.DescFactory;
import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Collector {
	static {
		jrds.log.JrdsLoggerFactory.initLog4J();
	}
	static final private Logger logger = Logger.getLogger(Collector.class);
	public static final int GRAPH_RESOLUTION = 300; // seconds
	private static final PropertiesManager pm = PropertiesManager.getInstance();

	public static void main(String[] args) throws Exception {
		Method m[] = jrds.GraphDesc.class.getDeclaredMethods();
		for(int i = 0; i < m.length ; i++) {
			//logger.debug(m[i].getName());
			Class MArgs[] = m[i].getParameterTypes();
			for(int j = 0; j < MArgs.length; j++) {
				//logger.debug("  " + MArgs[j]);
			}
		}
		Logger.getLogger(org.apache.commons.digester.Digester.class).setLevel(Level.ERROR);
		Logger.getLogger(org.apache.commons.digester.Digester.class).addAppender(logger.getAppender("jrds"));

		pm.join(new File("jrds.properties"));
		pm.update();
		//jrds.log.JrdsLoggerFactory.setOutputFile(pm.logfile);
		
		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm.getProperties());
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);
		
		DescFactory.init();
		final HostsList hl = HostsList.getRootGroup();
		//Logger.getLogger("org.apache").addAppender(logger.getAppender("jrds"));
		//System.getProperties().setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.digester.Digester","debug");
		//System.getProperties().setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.digester.Digester.sax","debug");
		//System.getProperties().setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
		//Logger.getLogger("org.apache").setLevel(Level.DEBUG);
		//Logger.getLogger("org.apache.commons.digester").setLevel(Level.DEBUG);
		//Logger.getLogger("org.apache.commons.digester.Digester.sax").setLevel(Level.DEBUG);
		logger.debug("Scanning dir");
		DescFactory.scanProbeDir(new File("config"));

		/*logger.setLevel(Level.ERROR);
		Logger.getRootLogger().setLevel(Level.ERROR);
		Logger.getLogger("jrds").setLevel(Level.ALL);
		logger.info("jrds' collector started");
		Logger.getLogger("org.snmp4j").setLevel(Level.ERROR);*/

		for(int i = 0; i< 1 ; i++) {
			hl.collectAll();
			System.gc();
			//Thread.sleep(10 * 1000L);
		}
		StoreOpener.stop();
	}
	
}

