/*
 * Created on 20 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

/**
 * A class to get pre-configue log4j loggers
 * by default it forces log level for org.snmp4j to warn
 *  @author Fabrice Bacchella
 */
public class JrdsLogger {
	static private final String APPENDER = "JRDS";
	static private final Level DEFAULTLEVEL = Level.DEBUG;
	private static PropertiesManager pm;
	static {
		Properties log4jprop = new Properties();
		
		Appender app = new ConsoleAppender(new org.apache.log4j.SimpleLayout(), ConsoleAppender.SYSTEM_ERR);
		app.setName(APPENDER);
		Logger logger = Logger.getLogger(JrdsLogger.class.getPackage().getName());
		logger.addAppender(app);
		logger.setLevel(DEFAULTLEVEL);
		
		Logger snmp4jLogger = Logger.getLogger("org.snmp4j");
		snmp4jLogger.setLevel(Level.WARN);
		snmp4jLogger.addAppender(app);
		
		pm = PropertiesManager.getInstance();
		logger.setLevel(pm.loglevel);
		PropertyConfigurator.configure(pm.getProperties());
	}

	private JrdsLogger() {
	}
	
	/**
	 * Return a configured logger, using the class package name for the appender name
	 * @param clazz the class of the objet to get a logger for
	 * @return configured logger
	 */
	static public org.apache.log4j.Logger getLogger(Class clazz){
		return Logger.getLogger(clazz.getPackage().getName());
	}

	/**
	 * Return a configured logger, using string used as argument the appender name
	 * @param clazz the appender name
	 * @return configured logger
	 */
	static public org.apache.log4j.Logger getLogger(String clazz){
		return Logger.getLogger(clazz);
	}
	
	/** Configure loggin to run in a web application
	 * the pat to the logfile is set by the propertie logile
	 */
	static public void doEmbeded()
	{
		
		Logger logger = Logger.getLogger(JrdsLogger.class.getPackage().getName());
		try {
			Layout l = new PatternLayout("[%d] %5p %c : %m%n");
			Appender app = new DailyRollingFileAppender(new org.apache.log4j.SimpleLayout(), pm.logfile,
						"'.'yyyy-ww");
			app.setLayout(l);
			app.setName(APPENDER);
			logger.removeAppender(APPENDER);
			logger.addAppender(app);

			logger.setLevel(pm.loglevel);
			PropertyConfigurator.configure(pm.getProperties());
		} catch (IOException e) {
			logger.error("Unable to open logfile " + pm.logfile + ": " + e.getLocalizedMessage());
		}
		
	}

}
