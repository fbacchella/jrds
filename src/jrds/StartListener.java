package jrds;

import java.io.File;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The class used to initialize the collecter and the logger if jrds is used as
 * a web application
 * @author Fabrice Bacchella
 *
 * TODO 
 */
public class StartListener implements ServletContextListener {
	static {
		jrds.JrdsLoggerConfiguration.initLog4J();
	}
	static private final Logger logger = Logger.getLogger(StartListener.class);
	static private boolean started = false;
	private static final Timer collectTimer = new Timer(true);



	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		Logger.getRootLogger().setLevel(Level.TRACE);
		//Resin launch the listener twice !
		if( ! started ) {
			try {
				ServletContext ctxt = arg0.getServletContext();
				logger.info("Starting jrds");
				PropertiesManager pm = new PropertiesManager();
				InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
				if(propStream != null) {
					pm.join(propStream);
				}

				String localPropFile = ctxt.getInitParameter("propertiesFile");
				if(localPropFile != null)
					pm.join(new File(localPropFile));

				pm.update();

				System.getProperties().setProperty("java.awt.headless","true");

				StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod);

				HostsList.getRootGroup().configure(pm);

				TimerTask collector = new TimerTask () {
					public void run() {
						HostsList.getRootGroup().collectAll();
					}
				};
				collectTimer.schedule(collector, 5000L, HostsList.getRootGroup().getResolution() * 1000L);
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
		HostsList.getRootGroup().getRenderer().finish();
		HostsList.getRootGroup().getStarters().stopCollect();
		BackEndCommiter.commit();
		logger.info("appplication jrds stopped");
		StoreOpener.stop();
	}

}
