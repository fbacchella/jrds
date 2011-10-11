package jrds.webapp;

import java.util.Properties;

import jrds.Probe;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.mockobjects.MokeProbe;
import jrds.standalone.JettyLogger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestDiscover  {
    static final private Logger logger = Logger.getLogger(TestDiscover.class);

    static ServletTester tester = null;

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml(false);
        System.setProperty("org.mortbay.log.class", "jrds.standalone.JettyLogger");
        Tools.setLevel(logger, Level.TRACE, JettyLogger.class.getName(), GetDiscoverHtmlCode.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
        Tools.setLevel(Level.INFO, "jrds.standalone.JettyLogger");
        Properties config = new Properties();
        config.put("tmpdir", "tmp");
        config.put("configdir", "tmp/config");
        config.put("autocreate", "true");
        config.put("rrddir", "tmp");
        config.put("libspath", "build/probes");

        tester = ToolsWebApp.getTestServer(config);
        Configuration c = (Configuration) tester.getAttribute(Configuration.class.getName());

        RdsHost h = new RdsHost();
        Probe<?,?> p = new MokeProbe<String, Number>();
        p.setHost(h);
        h.getProbes().add(p);
        c.getHostsList().addHost(h);
        c.getHostsList().addProbe(p);
        tester.addServlet(GetDiscoverHtmlCode.class, "/discoverhtml");

        tester.start();
    }

    @Test
    public void testDiscover() throws Exception
    {
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
