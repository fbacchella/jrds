package jrds.webapp;

import java.util.Properties;

import jrds.Tools;
import jrds.standalone.JettyLogger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.testing.ServletTester;

public class TestNoSecurity  {
    static final private Logger logger = Logger.getLogger(TestNoSecurity.class);

    ServletTester tester = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());
        Tools.setLevel(logger, Level.TRACE, JettyLogger.class.getName(), Status.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
    }

    @Before
    public void launchServer() throws Exception {
        Properties prop = new Properties();
        prop.put("security", "false");
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
        ToolsWebApp.doRequestGet(tester, "http://test/which", 200);
    }

}
