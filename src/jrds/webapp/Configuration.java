package jrds.webapp;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;

public class Configuration {
	static private final Logger logger = Logger.getLogger(Configuration.class);
	
	static final private AtomicInteger generation = new AtomicInteger(0);

	private PropertiesManager propertiesManager = new PropertiesManager();
	private HostsList hostsList = null;
	private Timer collectTimer;
	public int thisgeneration = generation.incrementAndGet();

	
	@SuppressWarnings("unchecked")
	public Configuration(ServletContext ctxt) {
		InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
		if(propStream != null) {
			propertiesManager.join(propStream);
		}

		for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getInitParameterNames())) {
			String value = ctxt.getInitParameter(attr);
			if(value != null)
				propertiesManager.setProperty(attr, value);
		}

		String localPropFile = ctxt.getInitParameter("propertiesFile");
		if(localPropFile != null)
			propertiesManager.join(new File(localPropFile));

		localPropFile = System.getProperty("jrds.propertiesFile");
		if(localPropFile != null)
			propertiesManager.join(new File(localPropFile));

		Pattern jrdsPropPattern = Pattern.compile("jrds\\.(.+)");
		Properties p = System.getProperties();
		for(String name: jrds.Util.iterate((Enumeration<String>)p.propertyNames())) {
			Matcher m = jrdsPropPattern.matcher(name);
			if(m.matches()) {
				String prop = System.getProperty(name);
				if(prop != null)
					propertiesManager.setProperty(m.group(1), prop);
			}
		}

		propertiesManager.update();

		if(logger.isTraceEnabled()) {
			dumpConfiguration(ctxt);
		}

		StoreOpener.prepare(propertiesManager.dbPoolSize, propertiesManager.syncPeriod, propertiesManager.timeout, propertiesManager.rrdbackend);

		hostsList = new HostsList(propertiesManager);
	}

	public void start() {
		collectTimer = new Timer("jrds-main-timer", true);
		TimerTask collector = new TimerTask () {
			public void run() {
				try {
					hostsList.collectAll();
				} catch (RuntimeException e) {
					logger.fatal("A fatal error occured during collect: ",e);
				}
			}
		};
		collectTimer.schedule(collector, 5000L, propertiesManager.step * 1000L);
	}

	public void stop() {
		collectTimer.cancel();
		collectTimer = null;
		hostsList.getRenderer().finish();
		hostsList.finished();
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

	/**
	 * @return the hostsList
	 */
	public HostsList getHostsList() {
		return hostsList;
	}

	/**
	 * @return the propertiesManager
	 */
	public PropertiesManager getPropertiesManager() {
		return propertiesManager;
	}

}
