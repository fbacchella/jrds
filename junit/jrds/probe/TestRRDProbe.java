package jrds.probe;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Tools;
import jrds.Util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRRDProbe {
    static final private Logger logger = Logger.getLogger(TestRRDProbe.class);
    static final private File rrdfile = new File("junit/ressources/rrdtool.rrd");
    static final private long start = 920802300;
    static final private long end = 920808900;


    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.Probe.RRDToolGraphNode", "jrds.graphe.RRDToolGraphNode");
    }

    @Test
    public void testBean() throws IOException, InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        RRDToolProbe p = new RRDToolProbe();
        p.setHost(new RdsHost("toto"));
        ProbeDesc pd = new ProbeDesc();
        pd.setProbeClass(p.getClass());
        pd.setName("Rrdtool");  
        pd.setProbeName("rrdtool");
        p.setPd(pd);
        p.setRrdfile(rrdfile);
        Assert.assertEquals("invalid rrdfile bean", p.getRrdfile(), pd.getBeanMap().get("rrdfile").getReadMethod().invoke(p));
        Assert.assertEquals("invalid rrdfile bean template ", p.getRrdfile().toString(), Util.parseTemplate("${attr.rrdfile}", p));
    }
    
    @Test
    public void test1() throws IOException {
        RRDToolProbe p = new RRDToolProbe();
        p.setHost(new RdsHost("toto"));
        ProbeDesc pd = new ProbeDesc();
        pd.setName("Rrdtool");	
        pd.setProbeName("rrdtool");
        p.setPd(pd);

        Assert.assertTrue("rrd native file can't be read", rrdfile.canRead());
        Assert.assertTrue("Configuration of the probe failed", p.configure(rrdfile));
        Assert.assertTrue("Check of the probe failed", p.checkStore());
        GraphDesc gd = new GraphDesc();
        gd.setGraphName("rrdtool");
        gd.setName("rrdtool");
        gd.add("speed", "speed", null, GraphDesc.LINE, Color.BLUE, "speed", GraphDesc.DEFAULTCF, false, null, null, null);
        gd.add("weight", "weight", null, GraphDesc.LINE, Color.GREEN, "weight", GraphDesc.DEFAULTCF, false, null, null, null);
        p.addGraph(gd);
        for(GraphNode gn: p.getGraphList()) {
            Graph g = gn.getGraph();
            g.setEnd(new Date(end * 1000));
            g.setStart(new Date(start * 1000));
            File outputFile =  new File("tmp", "rrdtool.png");
            OutputStream out = new FileOutputStream(outputFile);
            g.writePng(out);
            Assert.assertTrue("graph not created", outputFile.canRead());
        }
    }

}
