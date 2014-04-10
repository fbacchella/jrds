package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import jrds.Tools;

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

public class TestCheckValues {
    static final private Logger logger = Logger.getLogger(TestCheckValues.class);

    static ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        System.setProperty("org.mortbay.log.class", "jrds.standalone.JettyLogger");
        Tools.setLevel(Level.TRACE, TestCheckValues.class.getName(), "jrds.webapp.CheckValues");
    }

    @Before
    public void launchServer() throws Exception {
        URL configDirURL = Tools.class.getResource("/ressources/configfull/");

        Properties prop = new Properties();
        prop.setProperty("strictparsing", "true");
        prop.setProperty("readonly", "true");
        prop.put("configdir", configDirURL.getFile());
        prop.put("log.trace", "jrds.webapp");

        tester = ToolsWebApp.getMonoServlet(testFolder, prop, CheckValues.class, "/values/*");
        tester.start();        
    }

    @Test
    public void testFail() throws IOException, Exception {
        String url = "http://tester%s/values/localhost/truc/bad";
        HttpTester response = ToolsWebApp.doRequestGet(tester, url, 400);
        Assert.assertEquals("Invalid error message", "No matching probe", response.getReason());
    }

    @Test
    public void testSucessProbe() throws IOException, Exception {
        String url = "http://tester%s/values/localhost/ifx-lo0";
        HttpTester response = ToolsWebApp.doRequestGet(tester, url, 200);
        String content = response.getContent();
        Assert.assertTrue("last update not found", content.contains("Last update:"));
        Assert.assertTrue("last update age not found", content.contains("Last update age (ms):"));
        Assert.assertTrue("ifInErrors not found", content.contains("ifInErrors: NaN"));
        Assert.assertTrue("ifInUnknownProtos not found", content.contains("ifInUnknownProtos: NaN"));
    }

    @Test
    public void testSucessOneDS() throws IOException, Exception {
        String url = "http://tester%s/values/localhost/ifx-lo0/ifInMulticastPkts";
        HttpTester response = ToolsWebApp.doRequestGet(tester, url, 200);
        Assert.assertEquals("DS request invalid", "NaN", response.getContent().trim());
    }

    @Test
    public void testSucessOneDSWithCf() throws IOException, Exception {
        String url = "http://tester%s/values/localhost/ifx-lo0/ifInMulticastPkts/1000/max";
        HttpTester response = ToolsWebApp.doRequestGet(tester, url, 200);
        Assert.assertEquals("DS request invalid", "NaN", response.getContent().trim());
    }

}
