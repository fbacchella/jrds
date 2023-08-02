package jrds;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.DenyAllFilter;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class Log4JRule implements TestRule {

    private static final String APPENDERNAME = "jrdsAppender";
    private static final String DEFAULTLAYOUT = "[%d] %5p %c : %m%n";

    private static Appender jrdsAppender = null;
    private static boolean configured = false;
    private static final Map<Appender, Set<Category>> appenders;

    static {
        appenders = new HashMap<>();
        configure();
    }

    public synchronized static void configure() {
        if (! configured) {
            LogManager.getRootLogger().removeAllAppenders();
            jrdsAppender = new ConsoleAppender(new org.apache.log4j.PatternLayout(DEFAULTLAYOUT),
                                               ConsoleAppender.SYSTEM_OUT);
            jrdsAppender.setName(APPENDERNAME);

            LogManager.getRootLogger().addAppender(jrdsAppender);
            if ("true".equalsIgnoreCase(System.getProperty("jrds.hidelogs"))) {
                jrdsAppender.addFilter(new DenyAllFilter());
            }
            LogManager.getLoggerRepository().addHierarchyEventListener(new HierarchyEventListener() {
                @Override
                public synchronized void addAppenderEvent(Category cat, Appender appender) {
                    appenders.computeIfAbsent(appender, k -> new HashSet<>()).add(cat);
                }

                @Override
                public synchronized void removeAppenderEvent(Category cat,
                                                             Appender appender) {
                    appenders.computeIfAbsent(appender, k -> new HashSet<>()).remove(cat);
                    appenders.computeIfPresent(appender, (k,v) -> v.isEmpty() ? null : v);
                }
            });
            configured = true;
        }
    }

    private final Logger testlogger;

    public Log4JRule(Object test) {
        testlogger = LogManager.getLogger(test.getClass().getName());
        testlogger.setLevel(Level.TRACE);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    Set<Map.Entry<Category, Appender>> collected = new HashSet<>();
                    appenders.entrySet().stream()
                    .forEach(e -> e.getValue().stream()
                             .map(c -> new SimpleEntry<>(c, e.getKey()))
                             .forEach(collected::add));
                    collected.stream()
                    .filter( e -> jrdsAppender == null || ! jrdsAppender.equals(e.getValue()))
                    .forEach(e -> e.getKey().removeAppender(e.getValue()));
                }
            }
        };
    }

    /**
     * @return the testlogger
     */
    public org.slf4j.Logger getTestlogger() {
        return org.slf4j.LoggerFactory.getLogger(testlogger.getName());
    }

    public void setLevel(org.slf4j.event.Level level, String... loggers) {
        for (String logger: loggers) {
            Logger.getLogger(logger).setLevel(resolveLevel(level));
        }
    }

    public List<LoggingEvent> getLogChecker(String... loggers) {
        final List<LoggingEvent> logs = new ArrayList<>();
        Appender ta = new AppenderSkeleton() {
            @Override
            public synchronized void doAppend(LoggingEvent event) {
                super.doAppend(event);
            }

            @Override
            protected void append(LoggingEvent arg0) {
                logs.add(arg0);
            }

            public void close() {
                logs.clear();
            }

            public boolean requiresLayout() {
                return false;
            }
        };

        for(String loggername: loggers) {
            Logger logger = Logger.getLogger(loggername);
            logger.addAppender(ta);
            logger.setLevel(Level.TRACE);
            logger.setAdditivity(true);
        }
        return logs;
    }

    private Level resolveLevel(org.slf4j.event.Level l) {
        switch (l) {
        case TRACE:
            return org.apache.log4j.Level.TRACE;
        case DEBUG:
            return org.apache.log4j.Level.DEBUG;
        case WARN:
            return org.apache.log4j.Level.WARN;
        case INFO:
            return org.apache.log4j.Level.INFO;
        case ERROR:
            return org.apache.log4j.Level.ERROR;
        default:
            return null;
        }
    }

}
