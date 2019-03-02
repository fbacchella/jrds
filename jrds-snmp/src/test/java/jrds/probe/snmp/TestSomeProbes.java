package jrds.probe.snmp;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.configuration.ConfigObjectFactory;

public class TestSomeProbes {

    static final private Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.prepareXml(true);
        Tools.setLevel(logger, Level.TRACE, "jrds.PropertiesManager", "jrds.probe.snmp", "jrds.snmp");
    }

    /**
     * Used as a canary, as it collect a non-stored probe
     * @throws Exception
     */
    @Test
    public void testPartitionSpace() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);
        Tools.findDescs(pm);
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);

        Map<String, ProbeDesc<? extends Object>> probeDescMap = conf.setProbeDescMap();

        ProbeDesc<? extends Object> partitionSpace = probeDescMap.get("PartitionSpace");
        Assert.assertNotNull("PartitionSpace probe not found", partitionSpace);
        Collection<String> collected = partitionSpace.getCollectMapping().values();
        Assert.assertEquals(3, collected.size());
        Assert.assertTrue(collected.contains("Total"));
        Assert.assertTrue(collected.contains("Used"));
        Assert.assertTrue(collected.contains("hrStorageAllocationUnits"));
    }

    @Test
    public void testProcessStatusHostResources() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);
        Tools.findDescs(pm);
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);

        Map<String, ProbeDesc<? extends Object>> probeDescMap = conf.setProbeDescMap();

        ProbeDesc<? extends Object> partitionSpace = probeDescMap.get("ProcessStatusHostResources");
        Assert.assertNotNull("PartitionSpace probe not found", partitionSpace);
        Collection<String> collected = partitionSpace.getCollectMapping().values();
        Assert.assertEquals(1, collected.size());
        Assert.assertTrue(collected.contains("hrSWRunStatus"));
    }

}
