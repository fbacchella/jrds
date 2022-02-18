package jrds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.mockobjects.MokeProbe;
import jrds.starter.HostStarter;

public class TestHostsList {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    static final private String[] optionalstabs = { "@", "sumstab", "customgraph" };

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, HostsList.class.getName());
    }

    @Test
    public void testDefault() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "tabs=filtertab,customgraph,@,sumstab,servicestab,viewstab,hoststab,tagstab,adminTab");
        HostsList hl = new HostsList(pm);
        Assert.assertEquals("First tab not found", pm.tabsList.get(0), hl.getFirstTab());
        Assert.assertEquals("Missing tabs", pm.tabsList.size() - optionalstabs.length, hl.getTabsId().size());
        Assert.assertEquals(0, hl.getSecrets().list().collect(Collectors.toList()).size());
    }

    @Test
    public void testOther() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder);
        pm.setProperty("tabs", "filtertab");
        pm.update();
        HostsList hl = new HostsList(pm);
        Assert.assertEquals("First tab not found", pm.tabsList.get(0), hl.getFirstTab());
        Assert.assertEquals("Missing tabs", pm.tabsList.size(), hl.getTabsId().size());
    }

    @Test
    public void testTagsTab() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder);
        pm.setProperty("tabs", "filtertab");
        pm.update();
        HostsList hl = new HostsList(pm);
        Set<String> hostsTags = Collections.singleton("tag");
        Set<Tab> tabs = new HashSet<Tab>();
        hl.doTagsTabs(hostsTags, tabs);
        Tab tab = (Tab) tabs.toArray()[0];
        Assert.assertTrue(tab.isFilters());
    }

    @Test
    public void testCustomGraph() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder);
        pm.setProperty("tabs", "filtertab");
        pm.update();
        HostsList hl = new HostsList(pm);
        GraphDesc gd = new GraphDesc();
        gd.setName("truc");
        gd.setGraphTitle("title");
        Map<String, GraphDesc> gdmap = new HashMap<String, GraphDesc>();
        gdmap.put(gd.getName(), gd);
        Set<Tab> tabs = new HashSet<Tab>();
        Map<Integer, GraphNode> graphMap = new HashMap<Integer, GraphNode>();
        Map<String, GraphTree> graphTrees = new HashMap<String, GraphTree>();
        hl.doCustomGraphs(gdmap, graphMap, graphTrees, tabs);
        hl.addGraphs(graphMap.values());
        boolean found = false;
        GraphNode node = null;
        for(Tab t: tabs) {
            if(PropertiesManager.CUSTOMGRAPHTAB.equals(t.id)) {
                found = true;
                t.setHostlist(hl);
                node = t.getGraphTree().enumerateChildsGraph().get(0);
            }
        }
        Assert.assertNotNull(graphMap.get(node.hashCode()));
        Assert.assertTrue(found);
        Assert.assertNotEquals(graphTrees.size(), 0);
    }

    @Test
    public void testSecretHandler() throws IOException {
        String secretsPath = testFolder.newFile().getCanonicalPath();
        Files.delete(Paths.get(secretsPath));
        try (SecretStore sh = SecretStore.create(secretsPath)) {
            sh.add("somesecret", "thesecret".getBytes(StandardCharsets.UTF_8));
        }
        PropertiesManager pm = Tools.makePm(testFolder);
        pm.setProperty("secrets", secretsPath);
        pm.update();
        HostsList hl = new HostsList(pm);
        String thesecret = new String(hl.getSecrets().get("somesecret"), StandardCharsets.UTF_8);
        Assert.assertEquals("thesecret", thesecret);
        HostStarter hs = new HostStarter(new HostInfo("Moke"));
        hs.setParent(hl);
        Probe<?, ?> p = new MokeProbe<String, Number>();
        p.setParent(hs);
        String result = Util.parseTemplate("${secret.somesecret}", p);
        Assert.assertEquals("thesecret", result);
    }

}
