package jrds.standalone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import jrds.PropertiesManager;
import jrds.webapp.Configuration;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.webapp.WebAppContext;

public class Jetty extends CommandStarterImpl {

	static private final Logger logger = Logger.getLogger(Jetty.class);

	int port = 8080;
	String propFile = "jrds.properties";
	String webRoot = ".";
	
	public Jetty()  {
	}

	public void configure(Properties configuration) {
		logger.debug("Configuration: " + configuration);
		
		port = jrds.Util.parseStringNumber((String) configuration.getProperty("jetty.port"), Integer.class, port).intValue();
		propFile =  configuration.getProperty("propertiesFile", propFile);
		webRoot = configuration.getProperty("webRoot", webRoot);
	}
	
	public void start(String args[]) {
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());

		final Server server = new Server();
		Connector connector=new SelectChannelConnector();
		connector.setPort(port);

		//Let's try to start the connector before the application
		try {
			connector.open();
		} catch (IOException e) {
			throw new RuntimeException("Jetty server failed to start", e);
		}
		server.setConnectors(new Connector[]{connector});

		final WebAppContext webapp = new WebAppContext(webRoot, "/");
		webapp.setClassLoader(getClass().getClassLoader());
		Map<String, Object> initParams = new HashMap<String, Object>();
		initParams.put("propertiesFile", propFile);
		webapp.setInitParams(initParams);

		Thread t = new Thread() {
			/* (non-Javadoc)
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {
				try {
					ServletContext sc = webapp.getServletContext();
					Configuration c = (Configuration) sc.getAttribute(Configuration.class.getName());
					PropertiesManager pm = c.getPropertiesManager();
					if(pm.security) {
						Constraint constraint = new Constraint();
						constraint.setName(Constraint.__BASIC_AUTH);;
						constraint.setRoles(new String[]{"user","admin","moderator"});
						constraint.setAuthenticate(true);

						ConstraintMapping cm = new ConstraintMapping();
						cm.setConstraint(constraint);
						cm.setPathSpec("/*");

						SecurityHandler sh = new SecurityHandler();
						sh.setUserRealm(new HashUserRealm("MyRealm",pm.userfile));
						sh.setConstraintMappings(new ConstraintMapping[]{cm});
						
						webapp.addHandler(sh);

//						HandlerCollection handlers = new HandlerList();
//						//handlers.addHandler(sh);
//						for(Handler h: server.getHandlers()) {
//							//handlers.addHandler(h);
//							//server.removeHandler(h);
//							h.
//						}
//						//server.setHandler(handlers);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		};
		webapp.getServletContext().setAttribute(Thread.class.getName(), new Thread[] {t});

		ResourceHandler staticFiles=new ResourceHandler();
		staticFiles.setWelcomeFiles(new String[]{"index.html"});
		staticFiles.setResourceBase(webRoot);

		HandlerCollection handlers = new HandlerList();
		handlers.setHandlers(new Handler[]{staticFiles,webapp});
		server.setHandler(handlers);

		Thread finish = new Thread() {
		    public void run()
		    {
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
