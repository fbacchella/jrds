package jrds;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * This class is used to setup the log environment.
 * <p>
 * The normal starting point for logger configuration is initLog4J(). But
 * putAppender() can be used instead if log4j is already configured. It that's
 * the case, the following steps must be done:
 * <ul>
 * <li>Define the jrds appender using putAppender.
 * <li>Set additivity to false for the rootLoggers if this appender is used at
 * an higher level.
 * <li>Do not define a log file in the property file or PropertiesManager
 * object.
 * </ul>
 *
 * @author Fabrice Bacchella
 */
public class JrdsLoggerConfiguration {
    static public final String APPENDERNAME = "jrdsAppender";
    static public final String DEFAULTLOGFILE = "System.err";
    static public final String DEFAULTLAYOUT = "[%d] %5p %c : %m%n";
    static public Appender jrdsAppender = null;
    // The managed loggers list
    static public final Set<String> rootLoggers = new HashSet<>(Arrays.asList(new String[] { "jrds", "org.mortbay.log", "org.apache", "org.eclipse.jetty" }));
    // Used to check if jrds "own" the log configuration
    // null = we don't know yet
    static private Boolean logOwner = null;

    /**
     * The actual implementation of the logger helper methods is provided by this inner class.
     * This is done in order to avoid {@link ClassNotFoundException} being thrown in the event
     * log4j 2 API bridge is used by a calling application instead of the log4j 1.2 implementation.
     * This works because the inner class is loaded lazily and only if {@link JrdsLoggerConfiguration#logOwner}
     * is set to true {@code false}.
     */
    private static class JrdsLoggerConfigurationWorker {

        private static void configureLogger(String logname, Level level) {
            Logger logger = Logger.getLogger(logname);
            logger.setLevel(level);

            // Replace the appender, not optionally add it
            if (jrdsAppender != null) {
                Appender oldApp = logger.getAppender(jrdsAppender.getName());
                if (oldApp != null)
                    logger.removeAppender(oldApp);
                logger.addAppender(jrdsAppender);
                logger.setAdditivity(false);
            }

            // Keep the new logger name
            rootLoggers.add(logname);
        }

        private static void reset() {
            LogManager.shutdown();
            logOwner = null;
            jrdsAppender = null;
        }

    }

    private JrdsLoggerConfiguration() {

    }

    /**
     * Force an external log4j configuration, must be called before initLog4j
     */
    static public void setExternal() {
        logOwner = false;
    }

    static public void reset() {
        JrdsLoggerConfigurationWorker.reset();
    }

    /**
     * The method used to prepare a minimal set of logging configuration. This
     * should be used once. It does nothing if it detect that a appender already
     * exist for the logger <code>jrds</code>. The default logger is the system
     * error output and the default level is error.
     */
    static public void initLog4J() {
        // Do nothing if jrds is not allowed to setup logs
        if (isLogOwner()) {
            if (jrdsAppender == null) {
                jrdsAppender = new ConsoleAppender(new org.apache.log4j.SimpleLayout(), DEFAULTLOGFILE);
                jrdsAppender.setName(APPENDERNAME);
            }
            // Configure all the manager logger
            // Default level is debug, not a very good idea
            for (String loggerName : rootLoggers) {
                configureLogger(loggerName, Level.ERROR);
            }
        }
    }

    /**
     * This method prepare the log4j environment using the configuration in
     * jrds.properties. it uses the following properties
     * <ul>
     * <li><code>logfile</code>, used to define the log file, if not defined, no
     * appender is created</li>
     * <li><code>loglevel</code>, used to define the default loglevel</li>
     * <li><code>log.&lt;level&gt;</code>, followed by a comma separated list of
     * logger, to set the level of those logger to <code>level</code></li>
     * </ul>
     * @param loglevels 
     * @param loglevel 
     * @param logfile 
     *
     * @throws IOException
     */
    static public void configure(String logfile, Level loglevel, Map<Level, List<String>> loglevels) throws IOException {
        if (isLogOwner()) {
            if (logfile != null && ! logfile.isEmpty()) {
                jrdsAppender = new DailyRollingFileAppender(new PatternLayout(DEFAULTLAYOUT), logfile, "'.'yyyy-ww");
                jrdsAppender.setName(APPENDERNAME);
            }
            for (String logger : rootLoggers) {
                configureLogger(logger, loglevel);
            }

            for (Map.Entry<Level, List<String>> e : loglevels.entrySet()) {
                Level l = e.getKey();
                for (String logName : e.getValue()) {
                    Logger.getLogger(logName).setLevel(l);
                }
            }
        }
    }

    /**
     * This method is used to join other logger branch with the jrds' one and
     * use same setting if it's not already defined
     *
     * @param logname the logger name
     * @param level   the desired default level for this logger
     */
    static public void configureLogger(String logname, Level level) {
        if (isLogOwner()) {
            JrdsLoggerConfigurationWorker.configureLogger(logname, level);
        }
    }

    static private synchronized boolean isLogOwner() {
        // logOwner == null mean we don't know yet
        // if will be set to false if a logger called jrds already exist
        if (logOwner == null) {
            logOwner = LogManager.getLoggerRepository().exists("jrds") == null;
        }
        return logOwner;
    }

}
