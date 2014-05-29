package jrds.webapp;

import java.util.Properties;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.servlet.ServletTester;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestQueryParams {

    static final private Logger logger = Logger.getLogger(TestQueryParams.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        Tools.prepareXml(false);
        Tools.setLevel(logger, Level.TRACE, GetDiscoverHtmlCode.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
    }

    @Before
    public void prepare() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonQueryParams.class, "/queryparams");
        tester.start();
    }

    @Test
    public void testQueries() throws Exception
    {
        Response response = ToolsWebApp.doRequestGet(tester, "http://localhost/queryparams", 200);

        logger.trace(response.getContent());
        JSONObject qp = new JSONObject(response.getContent());
        for(String key: new String[] {"tab", "tabslist", "autoperiod", "begin", "end"}) {
            Assert.assertTrue(key + " not found", qp.has(key)); 
        }
    }

}
