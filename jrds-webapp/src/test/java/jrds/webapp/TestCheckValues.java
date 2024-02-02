package jrds.webapp;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.jetty.http.HttpTester.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;

public class TestCheckValues {

    static org.eclipse.jetty.servlet.ServletTester tester = null;

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
        logrule.setLevel(Level.TRACE, TestCheckValues.class.getName(), "jrds.webapp.CheckValues", "jrds.webapp", "jrds.PropertiesManager");
    }

    @Before
    public void launchServer() throws Exception {
        URL configDirURL = Paths.get("src/test/resources/configfull/").toUri().toURL();

        Properties prop = new Properties();
        prop.setProperty("strictparsing", "true");
        prop.setProperty("readonly", "true");
        prop.put("configdir", configDirURL.getFile());

        tester = ToolsWebApp.getMonoServlet(testFolder, prop, CheckValues.class, "/values/*");
        tester.start();
    }

    @Test
    public void testFail() throws Exception {
        String url = "http://tester/values/localhost/truc/bad";
        Response response = ToolsWebApp.doRequestGet(tester, url, 400);
        Assert.assertTrue(response.getContent().contains("No matching probe"));
    }

    @Test
    public void testSucessProbe() throws Exception {
        String url = "http://tester/values/localhost/ifx-lo0";
        Response response = ToolsWebApp.doRequestGet(tester, url, 200);
        String content = response.getContent();
        Assert.assertTrue("last update not found", content.contains("Last update:"));
        Assert.assertTrue("last update age not found", content.contains("Last update age (ms):"));
        Assert.assertTrue("ifInErrors not found", content.contains("ifInErrors: NaN"));
        Assert.assertTrue("ifInUnknownProtos not found", content.contains("ifInUnknownProtos: NaN"));
    }

    @Test
    public void testSucessOneDS() throws Exception {
        String url = "http://tester/values/localhost/ifx-lo0/ifInMulticastPkts";
        Response response = ToolsWebApp.doRequestGet(tester, url, 200);
        Assert.assertEquals("DS request invalid", "NaN", response.getContent().trim());
    }

    @Test
    public void testSucessOneDSWithCf() throws Exception {
        String url = "http://tester/values/localhost/ifx-lo0/ifInMulticastPkts/1000/max";
        Response response = ToolsWebApp.doRequestGet(tester, url, 200);
        Assert.assertEquals("DS request invalid", "NaN", response.getContent().trim());
    }

}
