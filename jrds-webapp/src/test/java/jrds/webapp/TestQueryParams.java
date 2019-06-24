package jrds.webapp;

import java.util.Properties;

import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.servlet.ServletTester;
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

public class TestQueryParams {

    ServletTester tester = null;

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
        logrule.setLevel(Level.TRACE, "jrds.webapp.Configuration\", \"jrds.webapp.JrdsServlet");
    }

    @Before
    public void prepare() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), JSonQueryParams.class, "/queryparams");
        tester.start();
    }

    @Test
    public void testQueries() throws Exception {
        Response response = ToolsWebApp.doRequestGet(tester, "http://localhost/queryparams", 200);

        logger.trace("{}", response.getContent());
        JSONObject qp = new JSONObject(response.getContent());
        for(String key: new String[] { "tab", "tabslist", "autoperiod", "begin", "end" }) {
            Assert.assertTrue(key + " not found", qp.has(key));
        }
    }

}
