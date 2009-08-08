package jrds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * The class used to initialize the collecter and the logger if jrds is used as
 * a web application
 * @author Fabrice Bacchella
 *
 * TODO 
 */
public class StartListener implements ServletContextListener {
	static private final Logger logger = Logger.getLogger(StartListener.class);
	static private boolean started = false;
	private static final Timer collectTimer = new Timer("jrds-main-timer", true);



	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@SuppressWarnings("unchecked")
	public void contextInitialized(ServletContextEvent arg0) {
		//System.out.println(arg0);
		//Logger.getLogger("jrds").setLevel(Level.TRACE);
		//logger.setLevel(Level.TRACE);
		//Resin launch the listener twice !
		if( ! started ) {
			try {
				jrds.JrdsLoggerConfiguration.initLog4J();
			} catch (IOException e2) {
				throw new RuntimeException(e2);
			}

			try {
				ServletContext ctxt = arg0.getServletContext();
				logger.info("Starting jrds");
				if(logger.isTraceEnabled()) {
					logger.trace("Dumping attributes");
					for (Enumeration<?> e = ctxt.getAttributeNames() ; e.hasMoreElements() ;)
					{
						String attr = (String) e.nextElement();
						Object o = ctxt.getAttribute(attr);
						logger.trace(attr + " = (" + o.getClass().getName() + ") " + o);
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

				System.setProperty("java.awt.headless","true");

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
				logger.fatal("Unable to start " + arg0.getServletContext().getServletContextName() + " because "+ ex +": ", ex);
			}

//			System.out.println("Default level: " + Logger.getRootLogger().getLevel());
//			Map<String, Appender> allapps= new HashMap<String, Appender>();
//			Enumeration<Logger> e = (Enumeration<Logger>)LogManager.getCurrentLoggers();
//			for(Logger l: Collections.list(e)) {
//				Enumeration<Appender> e1 = (Enumeration<Appender>)l.getAllAppenders();
//				if(e1.hasMoreElements() || l.getLevel() !=null) {
//					System.out.println(l.getName() + " " + l.getLevel());
//					for(Appender app: Collections.list(e1)) {
//						allapps.put(app.getName(), app);
//						System.out.println("    appender: " + app.getName());
//
//					}
//				}
//			}
//			System.out.println(allapps);
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
