/*-------------------------------------------------------------
 * $Id: $
 */
package jrds;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * This class is used to setup the log environment.<p>
 * The normal starting point for logger configuration is initLog4J(). But putAppender() can be used instead if log4j is already configured.
 * It that's the case, the following steps must be done:
	 * <ul>
	 * <li> Define the jrds appender using putAppender.
	 * <li> Set additivity to false for the rootLoggers if this appender is used at an higher level.
	 * <li> Do not define a log file in the property file or PropertiesManager object.
	 * </ul>
 * 
 * @author Fabrice Bacchella 
 * @version $Revision: 575 $,  $Date: 2009-08-22 22:38:42 +0200 (Sat, 22 Aug 2009) $
 */
public class JrdsLoggerConfiguration {
	static public String APPENDER = "jrds";
	static public final String DEFAULTLOGFILE = ConsoleAppender.SYSTEM_ERR;
	static public final String[] rootLoggers = new String[] {"jrds", "org.mortbay.log"};
	
	private JrdsLoggerConfiguration() {
		
	};

	/**
	 * The method used to prepare a minimal set of logging configuration.
	 * This should be used once. It does nothing if it detect that a appender already exist for the logger <code>jrds</code>.
	 * So to deactivate logging management
	 * in jrds and configure it outside (using log4j.properties for example), one should do 3 things:
	 * The default logger is the system error output and the default level is error.
	 * @throws IOException
	 */
	static public void initLog4J() throws IOException {
		//If already configured, don't do that again
		if(Logger.getLogger("jrds").getAllAppenders().hasMoreElements())
			return;
		Appender consoleAppender = new ConsoleAppender(new org.apache.log4j.SimpleLayout(), DEFAULTLOGFILE);
		consoleAppender.setName(APPENDER);
		putAppender(consoleAppender);
		//Default level is debug, not a very good idea
		for(String loggerName: rootLoggers) {
			Logger.getLogger(loggerName).setLevel(Level.ERROR);
		}
	}

	/**
	 * This method prepare the log4j environment using the configuration in jrds.properties.
	 * it uses the following properties
	 * <ul>
	 * <li> <code>logfile</code>, used to define the log file, if not defined, no appender is created
	 * <li> <code>loglevel</code>, used to define the default loglevel
	 * <li> <code>log.&lt;level&gt;</code>, followed by a comma separated list of logger, to set the level of those logger to <code>level</code>
	 * </li>
	 * @param pm a configured PropertiesManager object
	 * @throws IOException
	 */
	static public void configure(PropertiesManager pm) throws IOException {
		Logger.getLogger("jrds").setLevel(pm.loglevel);
		if(! "".equals(pm.logfile))
			setOutputFile(pm.logfile);
		for(Map.Entry<Level, List<String>> e: pm.loglevels.entrySet()) {
			Level l = e.getKey();
			for(String logName: e.getValue()) {
				Logger.getLogger(logName.trim()).setLevel(l);
			}
		}
	}

	/**
	 * Define an logfile, using a predefined patter : "[%d] %5p %c : %m%n", with a weekly rotation of this file, using a DailyRollingFileAppender
	 * @param logfile the path to the logfile
	 * @throws IOException
	 */
	static public void setOutputFile(String logfile) throws IOException {
		Appender newApp = null;
		Layout l = new PatternLayout("[%d] %5p %c : %m%n");
		newApp = new DailyRollingFileAppender(l, logfile, "'.'yyyy-ww");
		newApp.setName(APPENDER);
		putAppender(newApp);
	}

	/**
	 * Replaced all the defined appender of a logger with the jrds' one
	 * @param logName a logger name
	 */
	static public void joinAppender(String logName) {
		Logger logger = Logger.getLogger(logName);
		Appender app = Logger.getLogger("jrds").getAppender(APPENDER);
		Appender oldApp = logger.getAppender(app.getName());
		if(oldApp != null)
			logger.removeAppender(oldApp);
		logger.addAppender(app);
	}

	/**
	 * Use this appender to define the jrds appender
	 * @param app the new jrds' appender
	 */
	static public void putAppender(Appender app) {
		for(String loggerName: rootLoggers) {
			Logger logger = Logger.getLogger(loggerName);
			Appender oldApp = logger.getAppender(app.getName());
			if(oldApp != null)
				logger.removeAppender(oldApp);
			logger.addAppender(app);
		}
		APPENDER = app.getName();
	}
}
