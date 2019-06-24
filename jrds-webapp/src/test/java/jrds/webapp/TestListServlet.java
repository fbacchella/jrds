package jrds.webapp;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.servlet.ServletTester;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.factories.xml.NodeListIterator;

public class TestListServlet {

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
        logrule.setLevel(Level.TRACE, Status.class.getName());
    }

    @Ignore
    @Test
    public void testListServlet() throws Exception {
        Properties config = new Properties();
        config.put("tmpdir", new File(testFolder.getRoot(), "tmp").getCanonicalPath());
        config.put("configdir", new File(testFolder.getRoot(), "config").getCanonicalPath());
        config.put("autocreate", "true");
        config.put("rrddir", new File(testFolder.getRoot(), "rrd").getCanonicalPath());
        if(!Boolean.parseBoolean(System.getProperty("maven"))) {
            config.put("libspath", "desc");
        }

        ServletTester tester = ToolsWebApp.getTestServer(config);
        File cwd = new File(".");

        URLClassLoader cl = new URLClassLoader(new URL[] { cwd.toURI().toURL() });
        InputStream webxmlStream = Files.newInputStream(Paths.get("webapp/WEB-INF/web.xml"));
        JrdsDocument webxml = Tools.parseRessource(webxmlStream);
        for(JrdsElement n: new NodeListIterator<JrdsElement>(webxml, Tools.xpather.compile("/web-app/servlet/servlet-class"))) {
            String servletClassName = n.getTextContent().trim();
            @SuppressWarnings("unchecked")
            Class<? extends HttpServlet> sclass = (Class<? extends HttpServlet>) getClass().getClassLoader().loadClass(servletClassName);
            tester.addServlet(sclass, "/" + sclass.getCanonicalName());
            logger.trace("{}", sclass);
        }
        for(JrdsElement n: new NodeListIterator<JrdsElement>(webxml, Tools.xpather.compile("/web-app/listener/listener-class"))) {
            String className = n.getTextContent().trim();
            @SuppressWarnings("unchecked")
            Class<ServletContextListener> sclass = (Class<ServletContextListener>) getClass().getClassLoader().loadClass(className);
            logger.trace("{}", sclass);
        }
        cl.close();
    }

}
