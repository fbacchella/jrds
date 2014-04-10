package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jrds.PropertiesManager;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestReadElements {
    static final private Logger logger = Logger.getLogger(TestReadElements.class);

    static ServletTester tester = null;
    
    private final class TreeContent {
        List<JSONObject> graphs = new ArrayList<JSONObject>();
        List<JSONObject> trees = new ArrayList<JSONObject>();
        List<JSONObject> filters = new ArrayList<JSONObject>();
        List<JSONObject> nodes = new ArrayList<JSONObject>();
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        logger.setLevel(Level.TRACE);
        System.setProperty("org.mortbay.log.class", "jrds.standalone.JettyLogger");
        Tools.setLevel(logger.getLevel(), TestReadElements.class.getName(), "jrds.webapp");
    }

    @Before
    public void launchServer() throws Exception {
        URL configDirURL = Tools.class.getResource("/ressources/configfull/");
        logger.debug(configDirURL.getFile());

        Properties prop = new Properties();
        prop.setProperty("strictparsing", "true");
        prop.setProperty("readonly", "true");
        prop.put("configdir", configDirURL.getFile());
        prop.put("log.trace", "jrds.webapp");

        tester = ToolsWebApp.getMonoServlet(testFolder, prop, JSonTree.class, "/jsontree");
        tester.addServlet(JSonQueryParams.class, "/queryparams");
        tester.addServlet(JSonGraph.class, "/jsongraph");
        tester.addServlet(JSonDetails.class, "/details");
        tester.start();        
    }

    private JSONObject jsonquery(String query) throws IOException, Exception {
        String url = String.format("http://tester%s", query);
        HttpTester response = ToolsWebApp.doRequestGet(tester, url, 200);
        JSONObject content = new JSONObject(response.getContent());
        logger.debug(content);

        return(content);
    }

    private TreeContent scantree(JSONObject tree) throws IOException, Exception {
        TreeContent result = new TreeContent();
        int size = tree.getJSONArray("items").length();
        for(int i=0;i < size ; i++) {
            JSONObject item = tree.getJSONArray("items").getJSONObject(i);
            String type = item.getString("type");
            if("graph".equals(type)) {
                String id = item.getString("id").split("\\.")[1];
                JSONObject graphdetails = jsonquery("/jsongraph?id=" + id); 
                result.graphs.add(graphdetails);
            }
            else if("tree".equals(type)) {
                String id = item.getString("id").split("\\.")[1];
                JSONObject subtree = jsonquery("/jsontree?tree=" + id);
                logger.debug("subtree:  " +subtree.toString(2));
                TreeContent subresult = scantree(subtree);
                result.graphs.addAll(subresult.graphs);
                result.filters.addAll(subresult.filters);
                result.trees.add(item);
            }
            else if("filter".equals(type)) {
                result.filters.add(item);
            }
            else if("node".equals(type)) {
                result.nodes.add(item);
             }
            else {
                Assert.fail(type);
            }
        }
        return result;
    }

    @Test
    public void testQueryHost() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?host=localhost");
        Assert.assertEquals(4, tree.getJSONArray("items").length());   
        Assert.assertEquals(3, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testQueryFilter() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?filter=Localhost");
        Assert.assertEquals(21, tree.getJSONArray("items").length());   
        Assert.assertEquals(7, scantree(tree).graphs.size());
        Assert.assertEquals(2, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(12, scantree(tree).nodes.size());
    }

    @Test
    public void testServicesTab() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.SERVICESTAB);
        Assert.assertEquals(0, tree.getJSONArray("items").length());
        Assert.assertEquals(0, scantree(tree).graphs.size());
        Assert.assertEquals(0, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testViewTab() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.VIEWSTAB);
        Assert.assertEquals(11, tree.getJSONArray("items").length());        
        Assert.assertEquals(3, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(7, scantree(tree).nodes.size());
    }

    @Test
    public void testHostTab() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.HOSTSTAB);
        Assert.assertEquals(9, tree.getJSONArray("items").length());        
        Assert.assertEquals(4, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(4, scantree(tree).nodes.size());
    }

    @Test
    public void testTagsTab() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.TAGSTAB);
        Assert.assertEquals(1, tree.getJSONArray("items").length());        
        Assert.assertEquals(0, scantree(tree).graphs.size());
        Assert.assertEquals(0, scantree(tree).trees.size());
        Assert.assertEquals(1, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testCustomTab() throws IOException, Exception {
        JSONObject tree = jsonquery("/jsontree?tab=" + PropertiesManager.CUSTOMGRAPHTAB);
        Assert.assertEquals(2, tree.getJSONArray("items").length());        
        Assert.assertEquals(1, scantree(tree).graphs.size());
        Assert.assertEquals(1, scantree(tree).trees.size());
        Assert.assertEquals(0, scantree(tree).filters.size());
        Assert.assertEquals(0, scantree(tree).nodes.size());
    }

    @Test
    public void testTabList() throws IOException, Exception {
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
    public void testDetails() throws IOException, Exception {
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
