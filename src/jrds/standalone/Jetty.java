package jrds.standalone;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import jrds.PropertiesManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.webapp.WebAppContext;

public class Jetty extends CommandStarterImpl {

    static private final Logger logger = Logger.getLogger(Jetty.class);

    int port = 8080;
    String host;
    String propFileName = "jrds.properties";
    String webRoot = ".";

    public Jetty()  {
    }

    public void configure(Properties configuration) {
        logger.debug("Configuration: " + configuration);

        host = configuration.getProperty("jetty.host");
        port = jrds.Util.parseStringNumber(configuration.getProperty("jetty.port"), port).intValue();
        propFileName =  configuration.getProperty("propertiesFile", propFileName);
        webRoot = configuration.getProperty("webRoot", webRoot);
    }

    public void start(String args[]) {
        Logger.getRootLogger().setLevel(Level.ERROR);

        PropertiesManager pm = new PropertiesManager();
        File propFile = new File(propFileName);
        if(propFile.isFile())
            pm.join(propFile);
        pm.importSystemProps();
        try {
            pm.update();
        } catch (IllegalArgumentException e) {
            System.err.println("invalid configuration, can't start: " + e.getMessage());
            System.exit(1);
        }

        if(pm.withjmx) {
            doJmx(pm);
        }

        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");

        final Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        if (host != null) {
            connector.setHost(host);
        }
        connector.setPort(port);

        //Let's try to start the connector before the application
        try {
            connector.open();
        } catch (IOException e) {
            connector.close();
            throw new RuntimeException("Jetty server failed to start", e);
        }
        server.setConnectors(new Connector[]{connector});

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setResourceBase(webRoot);
        webapp.setClassLoader(getClass().getClassLoader());
        webapp.setInitParameter("propertiesFile", propFileName);

        ResourceHandler staticFiles=new ResourceHandler();
        staticFiles.setWelcomeFiles(new String[]{"index.html"});
        staticFiles.setResourceBase(webRoot);

        if(pm.security) {
            LoginService loginService = new HashLoginService("jrds",pm.userfile);
            server.addBean(loginService); 

            Authenticator auth = new BasicAuthenticator();
            Constraint constraint = new Constraint();
            constraint.setName("jrds");
            constraint.setRoles(new String[]{Constraint.ANY_ROLE});
            constraint.setAuthenticate(true);
            constraint.setDataConstraint(Constraint.DC_NONE);

            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec("/*");

            ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
            sh.setConstraintMappings(Collections.singletonList(cm));
            sh.setAuthenticator(auth);
            webapp.setSecurityHandler(sh);
        }

        HandlerCollection handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{staticFiles, webapp});
        server.setHandler(handlers);

        if(pm.withjmx || MBeanServerFactory.findMBeanServer(null).size() > 0) {
            MBeanServer mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
            server.addBean(new MBeanContainer(mbs));
            handlers.addHandler(new StatisticsHandler());    
        }

        //Properties are not needed any more
        pm = null;

        Thread finish = new Thread() {
            public void run() {
                try {
                    server.stop();
                } catch (Exception e) {
                    throw new RuntimeException("Jetty server failed to stop", e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(finish);

        try {
            server.start();            
            server.join();
        } catch (Exception e) {
            throw new RuntimeException("Jetty server failed to start", e);
        }
    }

    /* (non-Javadoc)
     * @see jrds.standalone.CommandStarterImpl#help()
     */
    @Override
    public void help() {
        System.out.println("Run an embedded web server, using jetty");
        System.out.print("The default listening port is " + port);
        System.out.println(". It can be specified using the property jetty.port");
        System.out.println("The jrds configuration file is specified using the property propertiesFile");
    }

}
