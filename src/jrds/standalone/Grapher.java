/*
 * Created on 25 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.standalone;

import java.io.File;
import java.util.Date;

import jrds.GraphTree;
import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.Renderer;
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
		jrds.JrdsLoggerConfiguration.initLog4J();
	}
	static final private Logger logger = Logger.getLogger(Grapher.class);

	public static void main(String[] args) throws Exception {
		String propFile = "jrds.properties";
		if(args.length == 1)
			propFile = args[0];
		PropertiesManager pm = new PropertiesManager(new File(propFile));
		jrds.JrdsLoggerConfiguration.configure(pm);

		System.getProperties().setProperty("java.awt.headless","true");
		System.getProperties().putAll(pm);
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, pm.rrdbackend);

		HostsList.getRootGroup().configure(pm);

		Date end = new Date();
		//end.setTime(end.getTime() - HostsList.getRootGroup().getResolution() * 1000L);
		Date begin = new Date(end.getTime() - 86400 * 1000);
		GraphTree graphTree = HostsList.getRootGroup().getGraphTreeByHost();
		Renderer r= new Renderer(10);
		for(jrds.RdsGraph g: graphTree.enumerateChildsGraph(null)) {
			logger.debug("Found graph for probe " + g.getProbe());
			//end = new Date(1000L * org.jrobin.core.Util.normalize(g.getProbe().getLastUpdate().getTime() / 1000L, HostsList.getRootGroup().getResolution()));
			try {
				r.render(g, begin, end);
			} catch (Exception e) {
				logger.error("Error " + e + " with " + g.getGraphTitle());
			}
		}
		for(jrds.Renderer.RendererRun rr: r.getWaitings()) {
			logger.debug(rr);
			rr.write();
			rr.clean();
		}
		r.finish();
		StoreOpener.stop();
	}
}
