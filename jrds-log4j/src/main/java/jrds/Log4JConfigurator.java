package jrds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;

import jrds.configuration.LogConfigurator;

public class Log4JConfigurator implements LogConfigurator {

    static public final String APPENDERNAME = "jrdsAppender";
    static public final String DEFAULTLOGFILE = "System.out";
    static public final String DEFAULTLAYOUT = "[%d] %5p %c : %m%n";

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(Log4JConfigurator.class);

    @Override
    public void configure(PropertiesManager pm) {
        // log4j uses the contextClassLoader, set it so it can uses fancy appenders
        Thread.currentThread().setContextClassLoader(pm.extensionClassLoader);

        String log4jXmlFile = pm.getProperty("log4jxmlfile", "");
        String log4jPropFile = pm.getProperty("log4jpropfile", "");
        if (log4jXmlFile != null && !log4jXmlFile.trim().isEmpty()) {
            Path xmlfile = Paths.get(log4jXmlFile.trim());
            if (Files.isReadable(xmlfile)) {
                BasicConfigurator.resetConfiguration();
                DOMConfigurator.configure(xmlfile.toString());
                logger.info("Configured with xml file {}", xmlfile);
            } else {
                logger.error("log4j xml file {} can't be read, log4j not configured", xmlfile);
            }
        } else if(log4jPropFile != null && !log4jPropFile.trim().isEmpty()) {
            Path propfile = Paths.get(log4jPropFile.trim());
            if (Files.isReadable(propfile)) {
                BasicConfigurator.resetConfiguration();
                PropertyConfigurator.configure(propfile.toString());
                logger.info("Configured with properties file {}", propfile);
            } else {
                logger.error("log4j properties file {} can't be read, log4j not configured", propfile);
            }
        } else {
            Map<Level, List<String>> loglevels = new HashMap<>();
            Level loglevel = null;
            String logfile = null;

            for(String ls: new String[] { "trace", "debug", "info", "error", "fatal", "warn" }) {
                Level l = Level.toLevel(ls);
                String param = pm.getProperty("log." + ls, "");
                if(!"".equals(param)) {
                    String[] loggersName = param.split(",");
                    List<String> loggerList = new ArrayList<>(loggersName.length);
                    for(String logger: loggersName) {
                        loggerList.add(logger.trim());
                    }
                    loglevels.put(l, loggerList);
                }
            }
            loglevel = Level.toLevel(pm.getProperty("loglevel", "info"));
            logfile = pm.getProperty("logfile");

            try {
                configure(logfile, loglevel, loglevels);
                logger.info("Configured using automatic configuration");
            } catch (IOException ex) {
                logger.error("Unable to set log file to {}: {}", logfile, ex.getMessage());
            }
        }
    }

    private void configure(String logfile, Level loglevel, Map<Level, List<String>> loglevels) throws IOException {
        Appender jrdsAppender;
        if (logfile != null && ! logfile.isEmpty()) {
            jrdsAppender = new DailyRollingFileAppender(new PatternLayout(DEFAULTLAYOUT), logfile, "'.'yyyy-ww");
        } else {
            jrdsAppender = new ConsoleAppender(new PatternLayout(DEFAULTLAYOUT), DEFAULTLOGFILE);
        }
        jrdsAppender.setName(APPENDERNAME);
        for (Map.Entry<Level, List<String>> e : loglevels.entrySet()) {
            Level l = e.getKey();
            for (String logName : e.getValue()) {
                Logger.getLogger(logName).setLevel(l);
            }
        }
        BasicConfigurator.configure(jrdsAppender);
    }

}
