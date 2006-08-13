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
import org.snmp4j.log.Log4jLogFactory;

/**
 * A class to get pre-configue log4j loggers
 * by default it forces log level for org.snmp4j to warn
 *  @author Fabrice Bacchella
 */
public class JrdsLogger {
	static private final String APPENDER = "jrds";
	static private Level DEFAULTLEVEL = Level.DEBUG;
	static private final String DEFAULTLOGFILE = ConsoleAppender.SYSTEM_ERR;
	static {
		try {
			Appender app = getAppender(false, DEFAULTLOGFILE);
			initLog(app, System.getProperties());
		} catch (IOException e) {
			Logger.getRootLogger().error("Unable to open " + DEFAULTLOGFILE + ": " + e.getLocalizedMessage());
		}
	}

	/**
	 * Private because all the method of the object are static
	 */
	private JrdsLogger() {

	}

	/**
	 * Configure logging to go to a file
	 * @param logfile The file to send log to
	 */
	static final public void setFileLogger(String logfile, Properties p)
	{
		try {
			Appender app = getAppender(true, logfile);
			initLog(app, p);
		} catch (IOException e) {
			Logger.getLogger(".").error("Unable to open " + logfile + ": " + e.getLocalizedMessage());
		}
	}

	static final private void initLog(Appender app, Properties p) {
		Logger logger = Logger.getLogger(APPENDER);
		logger.removeAppender(APPENDER);
		logger.addAppender(app);

		org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
		Logger snmp4jLogger = Logger.getLogger("org.snmp4j");
		snmp4jLogger.setLevel(Level.WARN);
		snmp4jLogger.removeAppender(APPENDER);
		snmp4jLogger.addAppender(app);

		org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
		Logger digesterLogger = Logger.getLogger("org.apache.commons");
		digesterLogger.setLevel(Level.WARN);
		digesterLogger.removeAppender(APPENDER);
		digesterLogger.addAppender(app);

		logger.setLevel(DEFAULTLEVEL);
		PropertyConfigurator.configure(p);
	}

	static final private Appender getAppender(boolean isFile, String logfile) throws IOException {
		Appender newApp = null;
		if(isFile) {
			Layout l = new PatternLayout("[%d] %5p %c : %m%n");
			newApp = new DailyRollingFileAppender(l, logfile, "'.'yyyy-ww");
		}
		else {
			newApp = new ConsoleAppender(new org.apache.log4j.SimpleLayout(), logfile);
		}
		newApp.setName(APPENDER);
		return newApp;
	}
}
