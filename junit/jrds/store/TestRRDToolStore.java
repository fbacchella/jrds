package jrds.store;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Period;
import jrds.Probe;
import jrds.Tools;
import jrds.mockobjects.GenerateProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.data.DataProcessor;

public class TestRRDToolStore {
    static final private Logger logger = Logger.getLogger(TestRRDToolStore.class);
    static final private File rrdfile = new File("junit/ressources/rrdtool.rrd");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, RRDToolStore.class.getCanonicalName(), "jrds.Probe");
    }

    @Test
    public void readDatasourcesNames() throws Exception {
        GenerateProbe.ChainedMap<Object> factoryArgs = GenerateProbe.ChainedMap.start(1).set("rrdfile", rrdfile.getCanonicalPath());
        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start(2)
                .set(StoreFactory.class, new RRDToolStoreFactory())
                .set(GenerateProbe.FACTORYCONFIG, factoryArgs);
        Probe<?,?> p = GenerateProbe.fillProbe(new GenerateProbe.EmptyProbe(), testFolder, args);
        p.getPd().add("speed", DsType.GAUGE);
        p.getPd().add("weight", DsType.GAUGE);
        Assert.assertTrue(p.checkStore());
        Period period = new Period("1999-03-07T13:00:00", "1999-03-07T13:15:00");
        DataProcessor dp = p.extract(ExtractInfo.get().make(period.getBegin(), period.getEnd()));
        String[] dsNames = dp.getSourceNames();
        Assert.assertEquals("data source weight not found", "weight", dsNames[0]);
        Assert.assertEquals("data source speed not found", "speed", dsNames[1]);
        Assert.assertEquals("Missing last values",2, p.getMainStore().getLastValues().size());
    }

}
