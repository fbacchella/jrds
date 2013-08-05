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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.RrdDb;
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

        Probe<?,?> p = Full.create(testFolder, Full.STEP);
        p.setTimeout(pm.timeout);
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
        glist.add("Empty/SumTest");
        Sum s = new Sum("A sum test", glist);
        s.configure(hl);
        PlottableMap ppm = s.getCustomData();
        ppm.configure(begin, end, Full.STEP);

        RrdDb db = new RrdDb(p.getRrdName());
        FetchData fd = db.createFetchRequest(ConsolFun.AVERAGE, begin, end).fetchData();        
        DataProcessor dp =  new DataProcessor(begin, end);
        dp.addDatasource("shade", fd);
        dp.setFetchRequestResolution(Full.STEP);
        dp.processData();    
        LinearInterpolator li = new LinearInterpolator(dp.getTimestamps(), dp.getValues("shade"));

        for(long i = begin; i < end - Full.STEP; i += Full.STEP) {
            Assert.assertEquals("Sum get wrong value", ppm.get("shade").getValue(i), li.getValue(i), 1e-7);
        }

        Graph g = new Graph(s);
        g.setPeriod(pr);
        File outputFile =  new File("tmp", "sum.png");
        OutputStream out = new FileOutputStream(outputFile);
        g.writePng(out);

    }

}
