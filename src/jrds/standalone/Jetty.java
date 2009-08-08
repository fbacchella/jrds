package jrds.standalone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jrds.StartListener;
import jrds.bootstrap.CommandStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;


public class Jetty implements CommandStarter {
	static private final Logger logger = Logger.getLogger(Jetty.class);

	int port = 8080;
	String propFile = "jrds.properties";
	String webRoot = ".";
	
	public Jetty()  {
	}

	public void configure(Map<String, String> configuration) {
		logger.debug("Configuration: " + configuration);
		port = jrds.Util.parseStringNumber(configuration.get("jetty.port"), Integer.class, 8080).intValue();
		propFile = configuration.get("propFile");
		webRoot = configuration.get("webRoot");
	}
	
	public void start() {
		try {
			jrds.JrdsLoggerConfiguration.initLog4J();
		} catch (IOException e) {
			throw new RuntimeException("Log configuration failed",e);
		}
		Logger.getRootLogger().setLevel(Level.ERROR);

		Server server = new Server();
		Connector connector=new SelectChannelConnector();
		connector.setPort(port);

		//Let's try to start the connector before the application
		try {
			connector.open();
		} catch (IOException e) {
			throw new RuntimeException("Jetty server failed to start", e);
		}
		server.setConnectors(new Connector[]{connector});

		WebAppContext webapp = new WebAppContext(webRoot, "/");
		webapp.setClassLoader(getClass().getClassLoader());
		server.addHandler(webapp);
		Map<String, Object> initParams = new HashMap<String, Object>();
		initParams.put("propertiesFile", propFile);
		webapp.setInitParams(initParams);

		ResourceHandler staticFiles=new ResourceHandler();
		staticFiles.setWelcomeFiles(new String[]{"index.html"});
		staticFiles.setResourceBase(webRoot);

		HandlerCollection handlers = new HandlerList();
		handlers.setHandlers(new Handler[]{staticFiles,webapp});
		server.setHandler(handlers);

		try {
			server.start();
			server.join();
		} catch (Exception e) {
			throw new RuntimeException("Jetty server failed to start", e);
		}
	}
}
