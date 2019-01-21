package jrds.probe.snmp;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.configuration.ConfigObjectFactory;
import jrds.snmp.SnmpConnection;

public class TestSomeProbes {

    static final private Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.prepareXml(true);
        Tools.setLevel(logger, Level.TRACE, "jrds.PropertiesManager");
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
        Collection<String> collected = partitionSpace.getCollectMapping().values();
        Assert.assertEquals(1, collected.size());
        Assert.assertTrue(collected.contains("hrSWRunStatus"));
    }

    @Test
    public void testNull() throws Exception {
        SnmpProbe p = new SnmpProbe() {

            @Override
            protected Set<OID> getOidSet() {
                return Collections.singleton(new OID(new int[] {0,0}));
            }
        };
        ProbeDesc<OID> pd = new ProbeDesc<>();
        pd.addSpecific(SnmpProbe.REQUESTERNAME, "RAW");
        p.setPd(pd);

        SnmpConnection cnx = new SnmpConnection() {

            @Override
            public boolean isStarted() {
                return false;
            }
            @Override
            public Snmp getSnmp() {
                return new Snmp() {

                    @Override
                    public ResponseEvent send(PDU pdu, Target target)
                                    throws IOException {
                        Assert.fail("Should never be called");
                        return null;
                    }

                };
            }
            @Override
            public PDUFactory getPdufactory() {
                return new DefaultPDUFactory();
            }
            @Override
            public Target getConnection() {
                return new CommunityTarget();
            }
        };
        p.getNewSampleValuesConnected(cnx);
    }

}
