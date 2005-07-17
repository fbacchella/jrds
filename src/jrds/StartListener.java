/*
 * Created on 11 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;
import org.jrobin.core.RrdBackendFactory;
import org.jrobin.core.RrdDbPool;

import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * The class used to initialize the collecter and the logger if jrds is used as
 * a web application
 * @author Fabrice Bacchella
 *
 * TODO 
 */
public class StartListener implements ServletContextListener {
	static final private Logger logger = JrdsLogger.getLogger(StartListener.class);
	static final PropertiesManager pm = PropertiesManager.getInstance();
	static final RrdDbPool dbpool = RrdDbPool.getInstance();
	static final HostsList hl = HostsList.getRootGroup();
	private ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
	
	
	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		
		try {
			InputStream propStream = arg0.getServletContext().getResourceAsStream("/WEB-INF/jrds.properties");
			if(propStream != null) {
				pm.join(propStream);
			}
			
			String localPropFile = arg0.getServletContext().getInitParameter("propertiesFile");
			if(localPropFile != null)
				pm.join(new File(localPropFile));
			
			pm.update();
			
			JrdsLogger.setFileLogger(pm.logfile);
			
			System.getProperties().setProperty("java.awt.headless","true");
			
			hl.append(new File(pm.configfilepath));
			
			logger.debug("propertiesFile = " + arg0.getServletContext().getInitParameter("propertiesFile"));
			logger.info("Application jrds started");
		
			dbpool.setCapacity(5);
			//RrdBackendFactory.setDefaultFactory(RrdFileBackendFactory.NAME);
			RrdBackendFactory.registerAndSetAsDefaultFactory(new RrdCachedFileBackendFactory());

			final Runnable collector = new Runnable () {
				private final HostsList lhl = hl;
				public void run() {
					try {
						lhl.collectAll();
					} catch (IOException e) {
						logger.error("Unable to launch collect: ", e);
					}
				}
			};
			timer.scheduleAtFixedRate(collector, 1, PropertiesManager.getInstance().resolution, TimeUnit.SECONDS);
		}
		catch (Exception ex) {
			logger.fatal("Unable to start " + arg0.getServletContext().getServletContextName(), ex);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		timer.shutdown();
		try {
			SnmpRequester.stop();
		} catch (IOException e) {
			logger.error("Strange problem while stopping snmp: ", e);
		}
		logger.info("appplication jrds stopped");
		logger.info("RrdDbPool efficency: " + dbpool.getPoolEfficency());
		logger.info("RrdDbPool hits: " + dbpool.getPoolHitsCount());
		logger.info("RrdDbPool requets: " + dbpool.getPoolRequestsCount());
		
	}
	
}
