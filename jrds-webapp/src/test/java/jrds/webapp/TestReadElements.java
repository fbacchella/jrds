package jrds.webapp;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

import fr.jrds.snmpcodec.OIDFormatter;
import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tools;

public class TestReadElements {
    static ServletTester tester = null;

    private static final class TreeContent {
        List<JSONObject> graphs = new ArrayList<>();
        List<JSONObject> trees = new ArrayList<>();
        List<JSONObject> filters = new ArrayList<>();
        List<JSONObject> nodes = new ArrayList<>();
    }

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        Tools.configure();
        OIDFormatter.register("/usr/share/snmp/mibs");
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, TestReadElements.class.getName(), "jrds.webapp");
    }

    @Before
    public void launchServer() throws Exception {
        URL configDirURL = Paths.get("src/test/resources/configfull/").toUri().toURL();

        Properties prop = new Properties();
        prop.setProperty("strictparsing", "false");
        prop.setProperty("readonly", "true");
        prop.put("configdir", configDirURL.getFile());

        tester = ToolsWebApp.getMonoServlet(testFolder, prop, JSonTree.class, "/jsontree");
        tester.addServlet(JSonQueryParams.class, "/queryparams");
        tester.addServlet(JSonGraph.class, "/jsongraph");
        tester.addServlet(JSonDetails.class, "/details");
        tester.start();
    }

    private JSONObject jsonquery(String query) throws Exception {
        String url = String.format("http://tester%s", query);
        Response response = ToolsWebApp.doRequestGet(tester, url, 200);
        JSONObject content = new JSONObject(response.getContent());
        logger.debug("content read is " + content);
        return (content);
    }

    private TreeContent scantree(JSONObject tree) throws Exception {
        TreeContent result = new TreeContent();
        int size = tree.getJSONArray("items").length();
        for(int i = 0; i < size; i++) {
            JSONObject item = tree.getJSONArray("items").getJSONObject(i);
            String type = item.getString("type");
            if("graph".equals(type)) {
                String id = item.getString("id").split("\\.")[1];
                JSONObject graphdetails = jsonquery("/jsongraph?id=" + id);
                result.graphs.add(graphdetails);
            } else if("tree".equals(type)) {
                String id = item.getString("id").split("\\.")[1];
                JSONObject subtree = jsonquery("/jsontree?tree=" + id);
                logger.debug("subtree:  " + subtree.toString(2));
                TreeContent subresult = scantree(subtree);
                result.graphs.addAll(subresult.graphs);
                result.filters.addAll(subresult.filters);
                result.trees.add(item);
            } else if("filter".equals(type)) {
                result.filters.add(item);
            } else if("node".equals(type)) {
                result.nodes.add(item);
            } else {
                Assert.fail(type);
            }
        }
        return result;
    }

    @Test
    public void testQueryHost() throws Exception {
        JSONObject tree = jsonquery("/jsontree?host=localhost");
        Assert.assertEquals(4, tree.getJSONArray("items").length());
        Assert.assertEquals(3, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testQueryFilter() throws Exception {
        JSONObject tree = jsonquery("/jsontree?filter=Localhost");
        Assert.assertEquals(21, tree.getJSONArray("items").length());
        Assert.assertEquals(7, scantree(tree).graphs.size());
        Assert.assertEquals(2, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(12, scantree(tree).nodes.size());
    }

    @Test
    public void testServicesTab() throws Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.SERVICESTAB);
        Assert.assertEquals(0, tree.getJSONArray("items").length());
        Assert.assertEquals(0, scantree(tree).graphs.size());
        Assert.assertEquals(0, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testViewTab() throws Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.VIEWSTAB);
        Assert.assertEquals(11, tree.getJSONArray("items").length());
        Assert.assertEquals(3, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(7, scantree(tree).nodes.size());
    }

    @Test
    public void testHostTab() throws Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.HOSTSTAB);
        Assert.assertEquals(9, tree.getJSONArray("items").length());
        Assert.assertEquals(4, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(4, scantree(tree).nodes.size());
    }

    @Test
    public void testTagsTab() throws Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.TAGSTAB);
        Assert.assertEquals(1, tree.getJSONArray("items").length());
        Assert.assertEquals(0, scantree(tree).graphs.size());
        Assert.assertEquals(0, scantree(tree).trees.size());
        Assert.assertEquals(1, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testCustomTab() throws Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.CUSTOMGRAPHTAB);
        Assert.assertEquals(2, tree.getJSONArray("items").length());
        Assert.assertEquals(1, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testTabList() throws Exception {
        JSONObject queryparams = jsonquery("/queryparams");
        JSONObject tabslist = queryparams.getJSONObject("tabslist");
        for(String key: new String[]{ PropertiesManager.FILTERTAB, 
                PropertiesManager.SUMSTAB,
                PropertiesManager.SERVICESTAB,
                PropertiesManager.VIEWSTAB,
                PropertiesManager.HOSTSTAB,
                PropertiesManager.TAGSTAB,
                PropertiesManager.ADMINTAB,
                PropertiesManager.CUSTOMGRAPHTAB}) {
            JSONObject jsontab = tabslist.getJSONObject(key);
            Assert.assertNotEquals(null, jsontab);
            Assert.assertNotEquals(null, jsontab.get("id"));
            Assert.assertNotEquals(null, jsontab.get("callback"));
            Assert.assertNotEquals(null, jsontab.get("isFilters"));
            Assert.assertNotEquals(null, jsontab.get("label"));
        }
    }

    @Test
    public void testDetails() throws Exception {
        JSONObject details = jsonquery("/details?pid=-554902849");
        Assert.assertEquals(-554902849, details.get("pid"));
        Assert.assertEquals("lo0", details.get("index"));
        Assert.assertEquals("localhost/ifx-lo0", details.get("probequalifiedname"));
        Assert.assertEquals("ifx-lo0", details.get("probeinstancename"));
        Assert.assertEquals("localhost", details.get("hostname"));
        Assert.assertEquals(17, details.getJSONArray("datastores").length());
        Assert.assertEquals(3, details.getJSONArray("graphs").length());
    }

}
