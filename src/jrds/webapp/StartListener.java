package jrds.webapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;

import org.apache.log4j.Logger;

/**
 * Used to start the application.<p>
 * Jrds search his configuration in different places, using the following order :
 * <ol>
 * <li>A file named <code>jrds.properties</code> in the <code>/WEB-INF</code> directory.
 * <li>The init parameters of the web app.
 * <li>A file whose path given by the init parameter <code>propertiesFile</code>.
 * <li>A file whose path is given by system property named <code>jrds.propertiesFile</code>.
 * <li>Any system property whose name start with <code>jrds.</code> .
 * </ol>
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
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
		//Resin and some others launch the listener twice !
		if( ! started ) {
			try {
				try {
					jrds.JrdsLoggerConfiguration.initLog4J();
				} catch (IOException e2) {
					throw new RuntimeException(e2);
				}

				ServletContext ctxt = arg0.getServletContext();
				PropertiesManager pm = new PropertiesManager();
				ctxt.setAttribute(PropertiesManager.class.getCanonicalName(), pm);
				InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
				if(propStream != null) {
					pm.join(propStream);
				}

				for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getInitParameterNames())) {
					String value = ctxt.getInitParameter(attr);
					if(value != null)
						pm.setProperty(attr, value);
				}

				String localPropFile = ctxt.getInitParameter("propertiesFile");
				if(localPropFile != null)
					pm.join(new File(localPropFile));

				localPropFile = System.getProperty("jrds.propertiesFile");
				if(localPropFile != null)
					pm.join(new File(localPropFile));

				Pattern jrdsPropPattern = Pattern.compile("jrds\\.(.+)");
				Properties p = System.getProperties();
				for(String name: jrds.Util.iterate((Enumeration<String>)p.propertyNames())) {
					Matcher m = jrdsPropPattern.matcher(name);
					if(m.matches()) {
						String prop = System.getProperty(name);
						if(prop != null)
							pm.setProperty(m.group(1), prop);
					}
				}

				pm.update();

				logger.info("Starting jrds");

				if(logger.isTraceEnabled()) {
					dumpConfiguration(ctxt);
				}

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
				collectTimer.schedule(collector, 5000L, pm.step * 1000L);
				started = true;
				logger.info("Application jrds started");
			}
			catch (Exception ex) {
				logger.fatal("Unable to start " + arg0.getServletContext().getServletContextName() + " because "+ ex +": ", ex);
				throw new RuntimeException(ex);
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

	@SuppressWarnings("unchecked")
	private void dumpConfiguration(ServletContext ctxt) {
		logger.trace("Dumping attributes");
		for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getAttributeNames())) {
			Object o = ctxt.getAttribute(attr);
			logger.trace(attr + " = (" + o.getClass().getName() + ") " + o);
		}
		logger.trace("Dumping init parameters");
		for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getInitParameterNames())) {
			String o = ctxt.getInitParameter(attr);
			logger.trace(attr + " = " + o);
		}
		logger.trace("Dumping system properties");
		Properties p = System.getProperties();
		for(String attr: jrds.Util.iterate((Enumeration<String>)p.propertyNames())) {
			Object o = p.getProperty(attr);
			logger.trace(attr + " = " + o);
		}		
	}

}
