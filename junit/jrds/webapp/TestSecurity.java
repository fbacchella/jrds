package jrds.webapp;

import java.util.Properties;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.ServletTester;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestSecurity  {
    static final private Logger logger = Logger.getLogger(TestSecurity.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, Status.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
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
