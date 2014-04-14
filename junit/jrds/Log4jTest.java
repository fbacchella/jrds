package jrds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Log4jTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    static final private String TANAME = "TANAME";
    static final private List<LoggingEvent> logs = new ArrayList<LoggingEvent>();
    static final Appender ta = new AppenderSkeleton() {
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

    @BeforeClass
    static public void configure() throws IOException {
        System.setProperty("java.io.tmpdir",  "tmp");
    }

    @Test
    public void testConfiguration() throws IOException {
        LogManager.getLoggerRepository().resetConfiguration();
        JrdsLoggerConfiguration.initLog4J();
        Logger jrdsLogger = LogManager.getLoggerRepository().exists("jrds");
        Assert.assertNotNull(jrdsLogger);
        PropertiesManager pm = Tools.makePm(testFolder, "loglevel=warn");
        JrdsLoggerConfiguration.configure(pm);
        Assert.assertEquals(pm.loglevel, jrdsLogger.getLevel());
    }

    @Test
    public void testOutsideConfiguration() throws IOException {
        LogManager.getLoggerRepository().resetConfiguration();
        ta.setName(TANAME);
        JrdsLoggerConfiguration.jrdsAppender = ta;
        Logger.getRootLogger().addAppender(ta);
        JrdsLoggerConfiguration.initLog4J();
        Assert.assertNotNull(LogManager.getLoggerRepository().exists("jrds"));
        PropertiesManager pm = Tools.makePm(testFolder, "loglevel=error");
        JrdsLoggerConfiguration.configure(pm);
        Logger l = Logger.getLogger("jrds");
        l.error("A message");
        l.debug("A debug message");
        Assert.assertEquals(1, logs.size());
    }

}
