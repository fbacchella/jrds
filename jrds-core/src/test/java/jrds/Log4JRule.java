package jrds;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.varia.DenyAllFilter;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class Log4JRule implements TestRule {

    private static boolean configured = false;
    private static final Map<Appender, Set<Category>> appenders;

    static {
        appenders = new HashMap<>();
        Log4JRule.configure();
    }

    public synchronized static void configure() {
        if (! configured) {
            JrdsLoggerConfiguration.jrdsAppender = new ConsoleAppender(new org.apache.log4j.PatternLayout(JrdsLoggerConfiguration.DEFAULTLAYOUT),
                                                                       ConsoleAppender.SYSTEM_OUT);
            JrdsLoggerConfiguration.jrdsAppender.setName(JrdsLoggerConfiguration.APPENDERNAME);
            LogManager.getRootLogger().setLevel(Level.WARN);
            JrdsLoggerConfiguration.initLog4J();
            if (System.getProperty("jrds.hidelogs") != null) {
                JrdsLoggerConfiguration.jrdsAppender.addFilter(new DenyAllFilter());
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
                             .map(c -> new SimpleEntry<Category, Appender>(c, e.getKey()))
                             .forEach(collected::add));
                    collected.stream()
                    .filter( e -> JrdsLoggerConfiguration.jrdsAppender == null || ! JrdsLoggerConfiguration.jrdsAppender.equals(e.getValue()))
                    .forEach(e -> e.getKey().removeAppender(e.getValue()));
                }
            }
        };
    }

    /**
     * @return the testlogger
     */
    public Logger getTestlogger() {
        return testlogger;
    }

    public void setLevel(Level level, String... loggers) {
        for (String logger: loggers) {
            Logger.getLogger(logger).setLevel(level);
        }
    }

}
