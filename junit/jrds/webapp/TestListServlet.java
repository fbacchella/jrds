package jrds.webapp;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.factories.xml.NodeListIterator;
import jrds.standalone.JettyLogger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.ServletTester;

public class TestListServlet {
    static final private Logger logger = Logger.getLogger(TestListServlet.class);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml(false);
        System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());
        Tools.setLevel(logger, Level.TRACE, JettyLogger.class.getName(), Status.class.getName());
    }

    @Test 
    public void testListServlet() throws Exception
    {
        ServletTester tester = ToolsWebApp.getTestServer(new Properties());

        File cwd = new File (".");
        ClassLoader cl = new URLClassLoader(new URL[] {cwd.toURI().toURL()});

        InputStream webxmlStream = cl.getResourceAsStream("web/WEB-INF/web.xml");
        JrdsDocument webxml = Tools.parseRessource(webxmlStream);
        for(JrdsElement n: new NodeListIterator<JrdsElement>(webxml, Tools.xpather.compile("/web-app/servlet/servlet-class"))) {
            String servletClassName = n.getTextContent().trim();
            @SuppressWarnings("unchecked")
            Class< ? extends HttpServlet> sclass = (Class<? extends HttpServlet>) getClass().getClassLoader().loadClass(servletClassName);
            tester.addServlet(sclass, "/" + sclass.getCanonicalName());
        }
        for(JrdsElement n: new NodeListIterator<JrdsElement>(webxml, Tools.xpather.compile("/web-app/listener/listener-class"))) {
            String className = n.getTextContent().trim();
            @SuppressWarnings({ "unused", "unchecked" })
            Class<ServletContextListener> sclass = (Class<ServletContextListener>) getClass().getClassLoader().loadClass(className);
        }
    }

}
