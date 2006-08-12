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
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
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
		//ClassLoader cl = Collector.class.getClassLoader();
		//while(cl != null) {
		//	logger.debug(cl);
		//	cl = cl.getParent();
		//}
		//ProbeClassLoader pl = new jrds.DescFactory.ProbeClassLoader();
		//pl.addURL(new URL("file:../jrdsExalead/build/jrdsexalead.jar"));
		//Class c = pl.loadClass("jrds.probe.exalead.Exalead");
		//logger.debug(c + " " + c.getClass().getClassLoader());
		//Class.forName("jrds.probe.exalead.Exalead");
		//System.exit(0);

		pm.join(new File("jrds.properties"));
		pm.update();
		jrds.log.JrdsLoggerFactory.setOutputFile(pm.logfile);

		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm.getProperties());
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);
		
		final HostsList hl = HostsList.getRootGroup();
		Logger logdigest = Logger.getLogger(org.apache.commons.digester.Digester.class);
		logdigest.setLevel(Level.ERROR);
		Logger.getLogger("org.apache.commons.digester.Digester.sax").setLevel(Level.ERROR);
		Logger.getLogger(org.apache.commons.digester.Digester.class).addAppender(jrds.log.JrdsLoggerFactory.app);
		logger.debug("Scanning dir");
		
		DescFactory.init();
		DescFactory.importJar("../jrdsExalead/build/jrdsexalead.jar");
		DescFactory.scanProbeDir(new File("config"));
		HostsList.getRootGroup().confLoaded();

		for(RdsHost h: hl.getHosts()) {
			for(Probe p: h.getProbes()) {
				p.getPd().dumpAsXml(p.getClass());
				for(jrds.RdsGraph g: p.getGraphList()) {
					g.getGraphDesc().dumpAsXml(g.getClass());
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

