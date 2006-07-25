/*
 * Created on 25 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.util.Date;

import jrds.DescFactory;
import jrds.GraphTree;
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
public class Grapher {
	static {
		jrds.log.JrdsLoggerFactory.initLog4J();
	}
	static final private Logger logger = Logger.getLogger(Grapher.class);
	private static final PropertiesManager pm = PropertiesManager.getInstance();

	public static void main(String[] args) throws Exception {
		pm.join(new File("jrds.properties"));
		pm.update();
		//jrds.log.JrdsLoggerFactory.setOutputFile(pm.logfile);
		
		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm.getProperties());
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);

		DescFactory.init();

		DescFactory.scanProbeDir(new File("config"));

		/*logger.setLevel(Level.ERROR);
		Logger.getRootLogger().setLevel(Level.ERROR);
		Logger.getLogger("jrds").setLevel(Level.ALL);
		Logger.getLogger("org.snmp4j").setLevel(Level.ERROR);
		System.getProperties().setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.digester.Digester","debug");
		System.getProperties().setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
		Logger.getLogger("org.apache.commons.digester").setLevel(Level.DEBUG);
		logger.info("jrds' grapher started");*/
		HostsList.getRootGroup().confLoaded();
		Date end = new Date();
		Date begin = new Date(end.getTime() - 86400 * 1000);
		GraphTree graphTree = HostsList.getRootGroup().getGraphTreeByHost();
		for(jrds.RdsGraph g: graphTree.enumerateChildsGraph(null/*ViewFilter.get("fdggfdl385"))*/)) {
			logger.debug("Found graph for probe " + g.getProbe());
			Date start = new Date();
			g.graph(begin, end);
			Date finish = new Date();
			long duration = finish.getTime() - start.getTime();
			logger.info("Graph " + g.getQualifieName() + " renderding  ran for " + duration + "ms");							
		}
		StoreOpener.stop();
	}
}
