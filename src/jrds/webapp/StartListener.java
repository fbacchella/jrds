package jrds.webapp;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerFactory;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import jrds.Configuration;
import jrds.PropertiesManager;
import jrds.StoreOpener;
import jrds.factories.ArgFactory;
import jrds.jmx.Management;

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
            System.setProperty("java.awt.headless","true");

            ServletContext ctxt = arg0.getServletContext();
            ctxt.setAttribute(StartListener.class.getName(), this);
            Properties p = readProperties(ctxt);
            Configuration conf = jrds.Configuration.configure(p);
            PropertiesManager pm = conf.getPropertiesManager();
            if(pm.security) {
                FilterRegistration filter = ctxt.addFilter("jrdssecurity", SecurityFilter.class);
                for(Entry<String, ? extends ServletRegistration> e: ctxt.getServletRegistrations().entrySet()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<HttpServlet> servletClass = (Class<HttpServlet>) getClass().getClassLoader().loadClass(e.getValue().getClassName());
                        Set<ServletSecurity> securitySet = ArgFactory.enumerateAnnotation(servletClass, ServletSecurity.class, HttpServlet.class);
                        if(securitySet.size() > 0) {
                            filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, e.getKey());
                            logger.debug("adding security to servlet " + e.getKey());
                        }
                    } catch (ClassNotFoundException ex) {
                    }
                }
            }
            //Register the mbean in MBeanServer if jmx activated
            if(MBeanServerFactory.findMBeanServer(null).size() > 0) {
                Management.register(ctxt);
            }
            started = true;
            logger.info("Application jrds started");
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
        if(started) {
            logger.info("Application jrds will stop");
            started = false;
            jrds.Configuration.stopConf();
            StoreOpener.stop();
            if(MBeanServerFactory.findMBeanServer(null).size() > 0) {
                Management.unregister();
            }
            logger.info("Application jrds stopped");
        }
    }

    public Properties readProperties(ServletContext ctxt) {
        Properties p = new Properties();

        InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
        if(propStream != null) {
            try {
                p.load(propStream);
            } catch (IOException ex) {
                logger.warn("Invalid properties stream " + propStream + ": " + ex);
            }
        }

        Enumeration<String> params = ctxt.getInitParameterNames();
        for(String attr: jrds.Util.iterate(params)) {
            String value = ctxt.getInitParameter(attr);
            if(value != null)
                p.setProperty(attr, value);
        }

        String localPropFile = ctxt.getInitParameter("propertiesFile");
        if(localPropFile != null)
            try {
                p.load(new FileReader(localPropFile));
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        return p;
    }

}
