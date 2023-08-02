package jrds.probe;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.GraphDesc.GraphType;
import jrds.GraphNode;
import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.Util;
import jrds.starter.HostStarter;
import jrds.store.RrdDbStoreFactory;

public class TestRRDProbe {

    static final private File rrdfile = new File(TestRRDProbe.class.getClassLoader().getResource("rrdtool.rrd").getFile());
    static final private long start = 920802300;
    static final private long end = 920808900;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Probe.RRDToolGraphNode", "jrds.graphe.RRDToolGraphNode", "javax.management", "sun.rmi");
    }

    @Test
    public void testBean() throws InvocationTargetException, IllegalArgumentException {
        RRDToolProbe p = new RRDToolProbe();
        p.setHost(new HostStarter(new HostInfo("toto")));
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setProbeClass(p.getClass());
        pd.setName("Rrdtool");
        pd.setProbeName("rrdtool");
        p.setPd(pd);
        p.setRrdfile(rrdfile);
        Assert.assertEquals("invalid rrdfile bean", p.getRrdfile(), pd.getBean("rrdfile").get(p));
        Assert.assertEquals("invalid rrdfile bean template ", p.getRrdfile().toString(), Util.parseTemplate("${attr.rrdfile}", p));
    }

    @Test
    public void test1() throws IOException, InvocationTargetException {
        RRDToolProbe p = new RRDToolProbe();
        p.setHost(new HostStarter(new HostInfo("toto")));
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.setName("Rrdtool");
        pd.setProbeName("rrdtool");
        p.setPd(pd);
        Map<String, String> empty = Collections.emptyMap();
        p.setMainStore(new RrdDbStoreFactory(), empty);

        Assert.assertTrue("rrd native file can't be read", rrdfile.canRead());
        Assert.assertTrue("Configuration of the probe failed", p.configure(rrdfile));
        Assert.assertTrue("Check of the probe failed", p.checkStore());
        GraphDesc gd = new GraphDesc();
        gd.setGraphName("rrdtool");
        gd.setName("rrdtool");
        gd.add(GraphDesc.getDsDescBuilder().setName("speed").setDsName("speed").setGraphType(GraphType.LINE).setColor(Color.BLUE).setLegend("speed"));
        gd.add(GraphDesc.getDsDescBuilder().setName("weight").setDsName("weight").setGraphType(GraphType.LINE).setColor(Color.GREEN).setLegend("weight"));
        p.addGraph(gd);
        for(GraphNode gn: p.getGraphList()) {
            Graph g = gn.getGraph();
            g.setEnd(new Date(end * 1000));
            g.setStart(new Date(start * 1000));
            File outputFile = new File(testFolder.getRoot(), "rrdtool.png");
            OutputStream out = new FileOutputStream(outputFile);
            g.writePng(out);
            Assert.assertTrue("graph not created", outputFile.canRead());
        }
    }

}
