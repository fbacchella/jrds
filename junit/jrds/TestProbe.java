package jrds;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.MokeProbe;
import jrds.starter.HostStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;

public class TestProbe {
    static final private Logger logger = Logger.getLogger(TestProbe.class);

    static public final class DummyProbe extends MokeProbe<String, Long> {
        public DummyProbe() {
            super();
        }
        @Override
        public boolean isCollectRunning() {
            return true;
        }           
        @Override
        public void modifySample(JrdsSample oneSample, Map<String, Long> values) {
            oneSample.setTime(new Date((getLastUpdate().getTime() + 1000) * 1000));
            super.modifySample(oneSample, values);
        }           
    };


    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.Probe", "jrds.ProbeDesc");
    }

    @Test
    public void testHighLow() throws Exception {
        ProbeDesc pd = new ProbeDesc();
        pd.setName("empty");
        pd.setProbeName("empty");
        Map<String, Object> dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds0");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("collecthigh", "high");
        dsMap.put("collectlow", "low");
        pd.add(dsMap);

        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start();
        args.set(ProbeDesc.class, pd).set(Probe.class, DummyProbe.class);
        @SuppressWarnings("unchecked")
        MokeProbe<String, Number> p = (MokeProbe<String, Number>) GenerateProbe.quickProbe(testFolder, args);

        HostStarter host = new HostStarter(new HostInfo("DummyHost"));
        host.setParent(Tools.getDefaultTimer());
        host.getHost().setHostDir(testFolder.newFolder("testDefault"));
        p.setHost(host);

        p.configure();
        Assert.assertTrue("Failed to create storage", p.checkStore());
        Map<String, Number> val = new HashMap<String, Number>();
        long high = 255L;
        long low = 64L;
        val.put("high", high);
        val.put("low", low);
        p.injectValues(val);
        p.collect();
        Assert.assertEquals("32 + 32 to 64 failed", (high << 32) + low, p.getLastValues().get("ds0").doubleValue(), 0.1);
    }

    @Test
    public void testDefault() throws Exception {
        ProbeDesc pd = new ProbeDesc();
        pd.setName("empty");
        pd.setProbeName("empty");
        Map<String, Object> dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds0");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("defaultValue", "1");
        pd.add(dsMap);
        dsMap.clear();
        dsMap.put("dsName", "ds1");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("defaultValue", "1");
        pd.add(dsMap);

        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start();
        args.set(ProbeDesc.class, pd).set(Probe.class, DummyProbe.class);
        @SuppressWarnings("unchecked")
        MokeProbe<String, Number> p = (MokeProbe<String, Number>) GenerateProbe.quickProbe(testFolder, args);

        HostStarter host = new HostStarter(new HostInfo("DummyHost"));
        host.setParent(Tools.getDefaultTimer());
        host.getHost().setHostDir(testFolder.newFolder("testDefault"));
        p.setHost(host);

        Assert.assertTrue("Failed to create storage", p.checkStore());
        Map<String, Number> val = new HashMap<String, Number>();
        val.put("ds1", 2L);

        p.checkStore();
        p.injectValues(val);
        p.collect();
        Assert.assertEquals("Default value is not inserted", 1, p.getLastValues().get("ds0").doubleValue(), 0.1);
        Assert.assertEquals("Default value overwrite read value", 2, p.getLastValues().get("ds1").doubleValue(), 0.1);
    }

}
