package jrds.webapp;

import java.util.Properties;

import org.eclipse.jetty.servlet.ServletTester;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;

public class TestSecurity {

    ServletTester tester = null;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, Status.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
    }

    @Before
    public void launchServer() throws Exception {
        Properties prop = new Properties();
        prop.put("security", "true");
        prop.put("defaultroles", "ANONYMOUS");
        prop.put("adminrole", "admin");

        tester = ToolsWebApp.getMonoServlet(testFolder, prop, Status.class, "/status");
        tester.addServlet(WhichLibs.class, "/which");

        tester.start();
    }

    @Test
    public void testStatus() throws Exception {
        ToolsWebApp.doRequestGet(tester, "http://test/status", 200);
    }

    @Test
    public void testWhich() throws Exception {
        ToolsWebApp.doRequestGet(tester, "http://test/which", 403);
    }

}
