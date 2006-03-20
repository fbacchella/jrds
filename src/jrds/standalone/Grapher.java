/*
 * Created on 25 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.util.Date;

import jrds.GraphFactory;
import jrds.HostsList;
import jrds.JrdsLogger;
import jrds.ProbeFactory;
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
public class Grapher {
	private static final PropertiesManager pm = PropertiesManager.getInstance();
	static {
		JrdsLogger.setFileLogger(pm.logfile);
	}
	private static final Logger logger = Logger.getLogger(Grapher.class);
	
	public static void main(String[] args) throws Exception {
		pm.join(new File("jrds.properties"));
		pm.update();
		
		System.getProperties().setProperty("java.awt.headless","true");
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);

		ProbeFactory.init();
		GraphFactory.init();

		final HostsList hl = HostsList.getRootGroup();
		hl.append(new File(pm.configfilepath));

		Logger.getLogger("").setLevel(Level.ALL);
		Logger.getLogger("jrds").setLevel(Level.ALL);
		Logger.getLogger("jrds.probe").setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.info("jrds' grapher started");
		
		Date end = new Date();
		Date begin = new Date(end.getTime() - 86400 * 1000);
		hl.graphAll(begin, end);
	}
}
