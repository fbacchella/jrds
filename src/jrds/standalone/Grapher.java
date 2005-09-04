/*
 * Created on 25 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;

import jrds.HostsList;
import jrds.JrdsLogger;
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
	private static final Logger logger = JrdsLogger.getLogger(Grapher.class);
	private static final PropertiesManager pm = PropertiesManager.getInstance();
	
	public static void main(String[] args) throws Exception {
		pm.join(new File("jrds.properties"));
		pm.update();
		
		System.getProperties().setProperty("java.awt.headless","true");
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);

		final HostsList hl = HostsList.getRootGroup();
		hl.append(new File(pm.configfilepath));

		JrdsLogger.getLogger("").setLevel(Level.ALL);
		JrdsLogger.getLogger("jrds").setLevel(Level.ALL);
		JrdsLogger.getLogger("jrds.probe").setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.info("jrds' grapher started");
		
		PrintWriter pw = new PrintWriter(System.err);
		Date end = new Date();
		Date begin = new Date(end.getTime() - 86400 * 1000);
		hl.graphAll(begin, end);
	}
}
