package jrds.webapp;

import java.util.Properties;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpTester.Response;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestTree {

    static final private Logger logger = Logger.getLogger(TestQueryParams.class);

    org.eclipse.jetty.servlet.ServletTester tester = null;

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
    public void prepareServlet() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonTree.class, "/jsontree");
        tester.start();
    }


    @Test
    public void testDiscover() throws Exception
    {
        Response response = ToolsWebApp.doRequestGet(tester, "http://localhost/jsontree?tab=hoststab", 200);

        logger.trace(response.getContent());
        JSONObject qp = new JSONObject(response.getContent());
        for(String key: new String[] {"identifier", "label", "items"}) {
            Assert.assertTrue(key + " not found", qp.has(key)); 
        }
    }

}
