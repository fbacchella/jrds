package jrds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import jrds.TestProbe.DummyProbe;
import jrds.graphe.Sum;
import jrds.mockobjects.Full;
import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.MokeProbe;
import jrds.store.ExtractInfo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.data.LinearInterpolator;

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

    @Test(expected=RuntimeException.class)
    public void emptysum() throws Exception {
        HostsList hl = new HostsList();
        hl.configure(new PropertiesManager());

        ArrayList<String> graphlist = new ArrayList<String>();
        graphlist.add("badhost/badgraph");
        Sum s = new Sum("emptysum", graphlist);
        s.configure(hl);
    }

    @Test
    public void getSum() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);        
        HostsList hl = new HostsList(pm);

        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start();
        args.set(ProbeDesc.class, Full.getPd()).set(Probe.class, DummyProbe.class).set(PropertiesManager.class, pm);

        @SuppressWarnings("unchecked")
        Probe<String, Number> p = (MokeProbe<String, Number>) GenerateProbe.quickProbe(testFolder, args);

        p.checkStore();
        long endSec = Full.fill(p);
        Period pr = Full.getPeriod(p, endSec);
        long begin = pr.getBegin().getTime() / 1000;
        long end = pr.getEnd().getTime() / 1000;

        GraphDesc gd = Full.getGd();
        gd.setGraphName("SumTest");
        gd.setName("SumTest");
        p.addGraph(gd);

        hl.addHost(p.getHost());
        hl.addProbe(p);

        ArrayList<String> glist = new ArrayList<String>();
        glist.add(p.getGraphList().iterator().next().getQualifiedName());
        Sum s = new Sum("A sum test", glist);
        s.configure(hl);
        PlottableMap ppm = s.getCustomData();
        ppm.configure(begin, end, Full.STEP);

        ExtractInfo ei = ExtractInfo.get().make(pr.getBegin(), pr.getEnd());
        DataProcessor dp = p.extract(ei);

        LinearInterpolator li = new LinearInterpolator(dp.getTimestamps(), dp.getValues("shade"));
        Assert.assertTrue("datasource shade not found", ppm.containsKey("shade"));
        Assert.assertTrue("datasource shade not found", ppm.containsKey("sun"));
        for(long i = begin; i < end - Full.STEP; i += Full.STEP) {
            Assert.assertEquals("Sum get wrong value", ppm.get("shade").getValue(i), li.getValue(i), 1e-7);
        }

        Graph g = new Graph(s);
        g.setPeriod(pr);
        File outputFile =  new File(testFolder.getRoot(), "sum.png");
        OutputStream out = new FileOutputStream(outputFile);
        g.writePng(out);
    }

}
