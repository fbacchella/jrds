package jrds.webapp;

import java.util.Properties;

import jrds.Tools;
import jrds.standalone.JettyLogger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestStats  {
    static final private Logger logger = Logger.getLogger(TestStats.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        logger.setLevel(Level.TRACE);
        System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());
        Tools.setLevel(new String[] {JettyLogger.class.getName(), Status.class.getName()}, logger.getLevel());
    }

    @Before
    public void prepareServlet() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), Status.class, "/status");
        tester.start();
    }

    @Test //(expected=NullPointerException.class)
    public void testStats() throws Exception {
        HttpTester response = ToolsWebApp.doRequestGet(tester, "http://tester/status", 200);
        Assert.assertTrue(response.getContent().contains("Hosts: 1"));
        Assert.assertTrue(response.getContent().contains("Probes: 1"));
        Assert.assertTrue(response.getContent().contains("Last collect:"));
        Assert.assertTrue(response.getContent().contains("Last running duration: 0s"));
    }


}
