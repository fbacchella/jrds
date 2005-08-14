package jrds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;

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
	static final HostsList hl = HostsList.getRootGroup();
	static private boolean started = false;
	private static final Timer collectTimer = new Timer(true);

	
	
	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		//Resin launch the listener twice !
		if( ! started ) {
			try {
				ServletContext ctxt = arg0.getServletContext();
				logger.info("Starting jrds");
				InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
				if(propStream != null) {
					pm.join(propStream);
				}
				
				String localPropFile = ctxt.getInitParameter("propertiesFile");
				logger.debug("propertiesFile = " + localPropFile);
				if(localPropFile != null)
					pm.join(new File(localPropFile));
				
				pm.update();
				
				JrdsLogger.setFileLogger(pm.logfile);
				
				System.getProperties().setProperty("java.awt.headless","true");
				
				StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);
				hl.append(new File(pm.configfilepath));
								
				TimerTask collector = new TimerTask () {
					private final HostsList lhl = hl;
					public void run() {
						try {
							lhl.collectAll();
						} catch (IOException e) {
							logger.error("Unable to launch collect: ", e);
						}
					}
				};
				collectTimer.schedule(collector, 5000L, PropertiesManager.getInstance().resolution * 1000L);
			}
			catch (Exception ex) {
				logger.fatal("Unable to start " + arg0.getServletContext().getServletContextName(), ex);
			}
			logger.info("Application jrds started");
			started = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		started = false;
		collectTimer.cancel();
		try {
			SnmpRequester.stop();
		} catch (IOException e) {
			logger.error("Strange problem while stopping snmp: ", e);
		}
		logger.info("appplication jrds stopped");
		StoreOpener.stop();
	}
	
}
