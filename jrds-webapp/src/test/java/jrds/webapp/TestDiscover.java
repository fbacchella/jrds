package jrds.webapp;

import java.util.Properties;

import org.eclipse.jetty.http.HttpTester.Response;
import org.eclipse.jetty.servlet.ServletTester;
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
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class TestDiscover {

    static ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public Log4JRule logrule = new Log4JRule(this);
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
        logrule.setLevel(Level.INFO, "jrds.standalone.JettyLogger");
    }

    @Before
    public void prepare() throws Exception {
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), GetDiscoverHtmlCode.class, "/discoverhtml");
        tester.start();
    }

    @Test
    public void testDiscover() throws Exception {
        Response response = ToolsWebApp.doRequestGet(tester, "http://localhost/discoverhtml", 200);

        logger.trace(response.getContent());
        JrdsDocument doc = new JrdsDocument(Tools.parseString(response.getContent()));
        JrdsElement root = doc.getRootElement();
        Assert.assertEquals("root element is not a div", "div", root.getNodeName());
        for(JrdsElement e: root.getChildElements()) {
            Assert.assertEquals("root element is not a div", "div", e.getNodeName());
            for(JrdsElement sub: e.getChildElements()) {
                String tag = sub.getNodeName();
                Assert.assertTrue("Unexpected HTML tag " + tag, "label".equals(tag) || "input".equals(tag) || "button".equals(tag));
            }
        }
    }

}
