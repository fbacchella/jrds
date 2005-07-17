/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;

import jrds.HostsList;
import jrds.JrdsLogger;
import jrds.PropertiesManager;
import jrds.RrdCachedFileBackendFactory;
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jrobin.core.RrdBackendFactory;
import org.jrobin.core.RrdDbPool;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Collector {
	public static final int GRAPH_RESOLUTION = 300; // seconds
	private static final Logger logger = JrdsLogger.getLogger(Collector.class);
	private static final PropertiesManager pm = PropertiesManager.getInstance();
	private static final RrdDbPool dbpool = RrdDbPool.getInstance();

	public static void main(String[] args) throws Exception {
		pm.join(new File("jrds.properties"));
		pm.update();

		RrdBackendFactory.registerAndSetAsDefaultFactory(new RrdCachedFileBackendFactory());
		
		System.getProperties().setProperty("java.awt.headless","true");
		HostsList.getRootGroup().append(new File(pm.configfilepath));
		final HostsList hl = HostsList.getRootGroup();
		JrdsLogger.getLogger("").setLevel(Level.ALL);
		JrdsLogger.getLogger("jrds").setLevel(Level.ALL);
		JrdsLogger.getLogger("probe").setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.info("jrds' collector started");
		JrdsLogger.getLogger("org.snmp4j").setLevel(Level.ALL);
		boolean doCollect = true;
		while(doCollect == true) {
			doCollect = false;
			Thread t = new Thread("Collector") {
				public void run() {
					try {
						SnmpRequester.start();
						hl.collectAll();
						SnmpRequester.stop();
						logger.info("One collect is done");
					}
					catch (Exception ex) {
						logger.error(ex.getLocalizedMessage(), ex);
					}
					
				}
			};
			t.start();
			logger.info("One collect was launched");
			if(doCollect)
				Thread.sleep(GRAPH_RESOLUTION * 1000L);
		}
		//hl.getThreadPool().awaitTermination(10, TimeUnit.SECONDS);
		//hl.getThreadPool().shutdown();
	}
	
}

