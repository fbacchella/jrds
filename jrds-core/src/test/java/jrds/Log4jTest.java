package jrds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Log4jTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public Log4JRule logrule = new Log4JRule(this);

    @Test
    public void testOutsideConfiguration() throws IOException {
        JrdsLoggerConfiguration.initLog4J();
        Path logPath = Paths.get(testFolder.getRoot().getCanonicalPath(), "test.log");
        PropertiesManager pm = Tools.makePm(testFolder, "loglevel=error", "logfile=" + logPath);
        Assert.assertEquals(Level.ERROR, pm.loglevel);
        JrdsLoggerConfiguration.configure(pm);
        Logger l = Logger.getLogger("jrds");
        l.error("A message");
        l.debug("A debug message");
        JrdsLoggerConfiguration.reset();
        List<String> logLines = Files.readAllLines(logPath);
        Assert.assertEquals("Unexepected logs found: " + logLines, 1, logLines.size());
    }

}
