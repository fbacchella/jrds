package jrds.webapp;

import java.util.Properties;

import org.eclipse.jetty.http.HttpTester.Response;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;

public class TestTree {

    org.eclipse.jetty.servlet.ServletTester tester = null;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, GetDiscoverHtmlCode.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
    }

    @Before
    public void prepareServlet() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonTree.class, "/jsontree");
        tester.start();
    }

    @Test
    public void testDiscover() throws Exception {
        Response response = ToolsWebApp.doRequestGet(tester, "http://localhost/jsontree?tab=hoststab", 200);

        logger.trace("{}", response.getContent());
        JSONObject qp = new JSONObject(response.getContent());
        for(String key: new String[] { "identifier", "label", "items" }) {
            Assert.assertTrue(key + " not found", qp.has(key));
        }
    }

}
