/*
 * Created on 24 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;

import jrds.HostsList;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
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
		String propFile = "jrds.properties";
		if(args.length == 1)
			propFile = args[0];
		PropertiesManager pm = new PropertiesManager(new File(propFile));
		jrds.JrdsLoggerConfiguration.configure(pm);

		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm);
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, pm.rrdbackend);

		HostsList hl = HostsList.getRootGroup();

		logger.debug("Scanning dir");

		HostsList.getRootGroup().configure(pm);

		if(false) {
			for(RdsHost h: hl.getHosts()) {
				for(Probe p: h.getProbes()) {
					try {
						p.getPd().dumpAsXml(p.getClass());
						for(jrds.GraphNode g: p.getGraphList()) {
							try {
								g.getGraphDesc().dumpAsXml(g.getClass());
							} catch (RuntimeException e) {
								logger.error("Unable to transform " + g);
							}
						}
					} catch (RuntimeException e) {
						logger.error("Unable to transform " + p);
					}
				}
			}
		}
		for(int i = 0; i< 1 ; i++) {
			hl.collectAll();
			System.gc();
			//Thread.sleep(10 * 1000L);
		}
		StoreOpener.stop();
	}

}

