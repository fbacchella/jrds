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
import org.snmp4j.log.Log4jLogFactory;

/**
 * TODO what this type is good for ?
 * @author bacchell
 * @version $Revision: $
 */
public class JrdsLoggerConfiguration {
	static public final String APPENDER = "jrds";
	static public final String DEFAULTLOGFILE = ConsoleAppender.SYSTEM_ERR;
	static public final String[] rootLoggers = new String[] {"jrds", "org.snmp4j", "org.mortbay.log"};

	static public void initLog4J() throws IOException {
		//If already configured, don't do that again
		if(Logger.getLogger("jrds").getAllAppenders().hasMoreElements())
			return;
		Appender consoleAppender = new ConsoleAppender(new org.apache.log4j.SimpleLayout(), DEFAULTLOGFILE);
		consoleAppender.setName(APPENDER);
		putAppender(consoleAppender);
		org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
		//The default log configuration is found in the PropertiesManager default values
		configure(new PropertiesManager());
	}

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

	static public void setOutputFile(String logfile) throws IOException {
		Appender newApp = null;
		Layout l = new PatternLayout("[%d] %5p %c : %m%n");
		newApp = new DailyRollingFileAppender(l, logfile, "'.'yyyy-ww");
		newApp.setName(APPENDER);
		putAppender(newApp);
	}

	static public void putAppender(Appender app) {
		for(String loggerName: rootLoggers) {
			Logger logger = Logger.getLogger(loggerName);
			Appender oldApp = logger.getAppender(app.getName());
			if(oldApp != null)
				logger.removeAppender(oldApp);
			logger.addAppender(app);

		}
	}
}
