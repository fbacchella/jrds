package jrds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.GetMoke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;

public class TestGraph {
    static final Logger logger = Logger.getLogger(TestGraph.class);
    HostsList hl;
    PropertiesManager pm;
    Probe<?,?> p;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException, URISyntaxException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.Graph");
    }

    @SuppressWarnings("unchecked")
    @Before
    public void prepare() throws Exception {
        pm = Tools.makePm(testFolder);
        hl = new HostsList(pm);
        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start().
                set(PropertiesManager.class, pm);
        p = GenerateProbe.quickProbe(testFolder, args);
        p.getPd().add("data", DsType.GAUGE);
        Assert.assertTrue("Failed to create probe " + p.getMainStore().getPath(),p.checkStore());
    }

    @Test 
    public void getBytes() throws Exception {
        GraphNode gn = new GraphNode(p, GetMoke.getGraphDesc());
        Period pr = new Period();
        Graph g = new Graph(gn);
        g.setPeriod(pr);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        g.writePng(out);
        Assert.assertTrue(out.size() > 0);
    }

    @Test
    public void compare() throws IOException, InvocationTargetException {
        GraphNode gn = new GraphNode(p, GetMoke.getGraphDesc());
        Period pr = new Period();
        Graph g1 = new Graph(gn);
        g1.setPeriod(pr);
        Graph g2 = new Graph(gn);
        g2.setPeriod(pr);
        Assert.assertEquals(g1.hashCode(), g2.hashCode());
        Assert.assertEquals(g1, g2);
    }

}
