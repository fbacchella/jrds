package jrds.webapp;

import java.util.Properties;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestStats  {
    static final private Logger logger = Logger.getLogger(TestStats.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, Status.class.getName());
    }

    @Before
    public void prepareServlet() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), Status.class, "/status");
        tester.start();
    }

    @Test //(expected=NullPointerException.class)
    public void testStats() throws Exception {
        Response response = ToolsWebApp.doRequestGet(tester, "http://tester/status", 200);
        Assert.assertTrue(response.getContent().contains("Hosts: 1"));
        Assert.assertTrue(response.getContent().contains("Probes: 1"));
        Assert.assertTrue(response.getContent().contains("Last collect:"));
        Assert.assertTrue(response.getContent().contains("Last running duration: 0s"));
    }

}
