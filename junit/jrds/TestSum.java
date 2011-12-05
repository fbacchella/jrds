package jrds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import jrds.graphe.Sum;
import jrds.mockobjects.Full;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class TestSum {
    static final private Logger logger = Logger.getLogger(TestSum.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.graphe.Sum");
        Tools.prepareXml();
    }

    @Test
    public void emptysum() throws Exception {
        HostsList hl = new HostsList();
        hl.configure(new PropertiesManager());

        ArrayList<String> graphlist = new ArrayList<String>();
        graphlist.add("badhost/badgraph");
        Sum s = new Sum("emptysum", graphlist);
        s.configure(hl);
        Graph g = s.getGraph();
        g.setPeriod(new Period());
        RrdGraphDef rgd = g.getRrdGraphDef();
        Assert.assertNotNull(rgd);
        RrdGraph graph = new RrdGraph(rgd);
        logger.debug(graph.getRrdGraphInfo().getHeight());
        logger.debug(graph.getRrdGraphInfo().getWidth());
        logger.debug(rgd.toString());
    }

    @Test
    public void getSum() throws IOException, TransformerException, ParserConfigurationException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("logevel", logger.getLevel().toString());
        pm.setProperty("rrdbackend", "FILE");
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("tabs", PropertiesManager.HOSTSTAB);

        pm.update();

        StoreOpener.prepare(pm.rrdbackend);

        HostsList hl = new HostsList(pm);

        //We don't want the file, just it's path
        File rrdFile = testFolder.newFile("fullmock.rrd");
        rrdFile.delete();

        Probe<?,?> p = Full.create(testFolder);
        long endSec = Full.fill(p);
        
        Period pr = Full.getPeriod(p, endSec);

        GraphDesc gd = Full.getGd();
        gd.setGraphName("SumTest");
        gd.setName("SumTest");
        p.addGraph(gd);

        hl.addHost(p.getHost());
        hl.addProbe(p);
        logger.trace(p.getGraphList());

        ArrayList<String> glist = new ArrayList<String>();
        glist.add("Empty/SumTest");
        Sum s = new Sum("A sum test", glist);
        s.configure(hl);
        Util.serialize(s.getGraphDesc().dumpAsXml(), System.out, null, null);
        PlottableMap ppm = s.getCustomData();
        long begin = pr.getBegin().getTime() / 1000;
        long end = pr.getEnd().getTime() / 1000;

        ppm.configure(begin, end, Full.STEP);
        for(long i = begin; i < end - Full.STEP; i += Full.STEP) {
            Assert.assertFalse(Util.delayedFormatString("a NaN found in the sum at step %d", i).toString(), Double.isNaN(ppm.get("shade").getValue(i)));
        }
        
        Graph g = new Graph(s);
        g.setPeriod(pr);
        File outputFile =  new File(testFolder.getRoot(), "sum.png");
        OutputStream out = new FileOutputStream(outputFile);
        g.writePng(out);

    }

}
