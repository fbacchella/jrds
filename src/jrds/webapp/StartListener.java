package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

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

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0) {
		//Resin and some others launch the listener twice !
		if( ! started ) {
			try {
				jrds.JrdsLoggerConfiguration.initLog4J();
			} catch (IOException e2) {
				throw new RuntimeException(e2);
			}

			System.setProperty("java.awt.headless","true");

			ServletContext ctxt = arg0.getServletContext();
			Configuration c = new Configuration(ctxt);
			c.start();
			ctxt.setAttribute(Configuration.class.getName(), c);
			started = true;
			logger.info("jrds started");
			logger.info("Application jrds started");
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0) {
		if(started) {
			started = false;
			ServletContext ctxt = arg0.getServletContext();
			Configuration c = (Configuration) ctxt.getAttribute(Configuration.class.getName());
			c.stop();
			StoreOpener.stop();
			logger.info("appplication jrds stopped");
		}
	}

}
