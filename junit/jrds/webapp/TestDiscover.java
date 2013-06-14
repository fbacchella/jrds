package jrds.webapp;

import java.util.Properties;

import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
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

public class TestDiscover  {
    static final private Logger logger = Logger.getLogger(TestDiscover.class);

    static ServletTester tester = null;

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
        tester = ToolsWebApp.getMonoServlet(testFolder, new Properties(), GetDiscoverHtmlCode.class, "/discoverhtml");
        tester.start();        
    }

    @Test
    public void testDiscover() throws Exception {
        HttpTester response = ToolsWebApp.doRequestGet(tester, "http://localhost/discoverhtml", 200);

        logger.trace(response.getContent());
        JrdsDocument doc = new JrdsDocument(Tools.parseString(response.getContent()));
        JrdsElement root =  doc.getRootElement();
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
