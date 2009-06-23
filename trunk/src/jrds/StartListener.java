package jrds;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
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
	private static final Timer collectTimer = new Timer("jrds-main-timer", true);



	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		Logger.getLogger("jrds").setLevel(Level.TRACE);
		logger.setLevel(Level.TRACE);
		//Resin launch the listener twice !
		if( ! started ) {
			try {
				ServletContext ctxt = arg0.getServletContext();
				logger.info("Starting jrds");
				if(logger.isTraceEnabled()) {
					logger.trace("Dumping attributes");
					for (Enumeration<?> e = ctxt.getAttributeNames() ; e.hasMoreElements() ;)
					{
						String attr = (String) e.nextElement();
						Object o = ctxt.getAttribute(attr);
						logger.trace(attr + " = " + o);
					}
					logger.trace("Dumping init parameters");
					for (Enumeration<?> e = ctxt.getInitParameterNames() ; e.hasMoreElements() ;)
					{
						String attr = (String) e.nextElement();
						Object o = ctxt.getInitParameter(attr);
						logger.trace(attr + " = " + o);
					}
					logger.trace("Dumping properties");
					Properties p = System.getProperties();
					for (Enumeration<?> e = p.propertyNames() ; e.hasMoreElements() ;)
					{
						String attr = (String) e.nextElement();
						Object o = p.getProperty(attr);
						logger.trace(attr + " = " + o);
					}
				}
				PropertiesManager pm = new PropertiesManager();
				ctxt.setAttribute(PropertiesManager.class.getCanonicalName(), pm);
				InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
				if(propStream != null) {
					pm.join(propStream);
				}

				String localPropFile = ctxt.getInitParameter("propertiesFile");
				if(localPropFile != null)
					pm.join(new File(localPropFile));

				localPropFile = System.getProperty("propertiesFile");
				if(localPropFile != null)
					pm.join(new File(localPropFile));

				pm.update();

				System.getProperties().setProperty("java.awt.headless","true");

				StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, pm.rrdbackend);

				HostsList.getRootGroup().configure(pm);

				TimerTask collector = new TimerTask () {
					public void run() {
						try {
							HostsList.getRootGroup().collectAll();
						} catch (RuntimeException e) {
							logger.fatal("A fatal error occured during collect: ",e);
						}
					}
				};
				collectTimer.schedule(collector, 5000L, HostsList.getRootGroup().getStep() * 1000L);
				started = true;
				logger.info("Application jrds started");
			}
			catch (Exception ex) {
				logger.fatal("Unable to start " + arg0.getServletContext().getServletContextName(), ex);
			}
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
		jrds.HostsList.purge();
		StoreOpener.stop();
		logger.info("appplication jrds stopped");
	}

}
