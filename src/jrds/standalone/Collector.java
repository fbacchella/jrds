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

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Collector {
	static {
		jrds.JrdsLoggerConfiguration.initLog4J();
	}
	static final private Logger logger = Logger.getLogger(Collector.class);

	public static void main(String[] args) throws Exception {
		PropertiesManager pm = new PropertiesManager(new File("jrds.properties"));
		jrds.JrdsLoggerConfiguration.configure(pm);

		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm);
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);
		
		HostsList hl = HostsList.getRootGroup();

		logger.debug("Scanning dir");
		
		HostsList.getRootGroup().configure(pm);

		/*for(RdsHost h: hl.getHosts()) {
			for(Probe p: h.getProbes()) {
				p.getPd().dumpAsXml(p.getClass());
				for(jrds.RdsGraph g: p.getGraphList()) {
					g.getGraphDesc().dumpAsXml(g.getClass());
				}
			}
		}*/
		for(int i = 0; i< 1 ; i++) {
			hl.collectAll();
			System.gc();
			//Thread.sleep(10 * 1000L);
		}
		StoreOpener.stop();
	}
	
}

