package jrds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import jrds.mockobjects.GetMoke;
import jrds.objects.probe.Probe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GraphTest {
    static final Logger logger = Logger.getLogger(GraphTest.class);
    static HostsList hl;

    @BeforeClass
    static public void configure() throws IOException, URISyntaxException {
        Tools.configure();
        Tools.setLevel(logger, Level.ERROR, "jrds.Graph");
        PropertiesManager pm = Tools.getCleanPM();
        hl = new HostsList(pm);
    }

    @Test 
    public void getBytes() throws IOException {
        Probe<?,?> p = GetMoke.getProbe();
        GraphNode gn = new GraphNode(p, GetMoke.getGraphDesc());
        Period pr = new Period();
        Graph g = new Graph(gn);
        g.setPeriod(pr);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        g.writePng(out);
        Assert.assertTrue(out.size() > 0);
    }

    @Test
    public void compare() throws IOException {
        Probe<?,?> p = GetMoke.getProbe();
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
