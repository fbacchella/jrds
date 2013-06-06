package jrds.store;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Probe;
import jrds.StoreOpener;
import jrds.Tools;
import jrds.mockobjects.GenerateProbe;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestRRDToolStore {
    static final private Logger logger = Logger.getLogger(TestRRDToolStore.class);
    static final private File rrdfile = new File("junit/ressources/rrdtool.rrd");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE);
        StoreOpener.prepare("FILE");
    }

    @Test
    public void readDatasourcesNames() throws Exception {
        GenerateProbe.ChainedMap factoryArgs = GenerateProbe.ChainedMap.start(1).set("rrdfile", rrdfile.getCanonicalPath());
        GenerateProbe.ChainedMap args = GenerateProbe.ChainedMap.start(2)
                                            .set(AbstractStoreFactory.class, new RRDToolStoreFactory())
                                            .set(GenerateProbe.FACTORYCONFIG, factoryArgs);
        Probe<?,?> p = GenerateProbe.fillProbe(new GenerateProbe.EmptyProbe(), testFolder, args);
        Assert.assertTrue(p.getMainStore().checkStoreFile());
        Extractor e = p.getMainStore().fetchData();
        String[] dsNames = e.getNames();
        Assert.assertEquals("data source speed not found", "speed", dsNames[0]);
        Assert.assertEquals("data source weight not found", "weight", dsNames[1]);
    }

}
