package jrds.webapp;

import java.util.Properties;

import jrds.Tools;
import jrds.standalone.JettyLogger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestQueryParams {

    static final private Logger logger = Logger.getLogger(TestQueryParams.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml(false);
        System.setProperty("org.mortbay.log.class", "jrds.standalone.JettyLogger");
        Tools.setLevel(logger, Level.TRACE, JettyLogger.class.getName(), GetDiscoverHtmlCode.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
        Tools.setLevel(Level.INFO, "jrds.standalone.JettyLogger");
    }

    @Before
    public void prepare() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonQueryParams.class, "/queryparams");
        tester.start();
    }

    @Test
    public void testQueries() throws Exception
    {
        HttpTester response = ToolsWebApp.doRequestGet(tester, "http://localhost/queryparams", 200);

        logger.trace(response.getContent());
        JSONObject qp = new JSONObject(response.getContent());
        for(String key: new String[] {"tab", "tabslist", "autoperiod", "begin", "end"}) {
            Assert.assertTrue(key + " not found", qp.has(key)); 
        }
    }

}
