package jrds;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.slf4j.event.Level;

import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.MokeProbe;
import jrds.starter.HostStarter;

public class TestProbe {

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
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Probe", "jrds.ProbeDesc");
    }

    @Test
    public void testHighLow() throws Exception {
        ProbeDesc<String> pd = new ProbeDesc<String>();
        pd.setName("empty");
        pd.setProbeName("empty");
        pd.add(ProbeDesc.getDataSourceBuilder("ds0", DsType.COUNTER).setCollectKeyHigh("high").setCollectKeyLow("low"));

        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start();
        args.set(ProbeDesc.class, pd).set(Probe.class, DummyProbe.class);
        MokeProbe<String, Number> p = GenerateProbe.quickProbe(testFolder, args);

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
        ProbeDesc<String> pd = new ProbeDesc<String>();
        pd.setName("empty");
        pd.setProbeName("empty");

        pd.add(ProbeDesc.getDataSourceBuilder("ds0", DsType.COUNTER).setDefaultValue(1.0));
        pd.add(ProbeDesc.getDataSourceBuilder("ds1", DsType.COUNTER).setDefaultValue(1.0));

        GenerateProbe.ChainedMap<Object> args = GenerateProbe.ChainedMap.start();
        args.set(ProbeDesc.class, pd).set(Probe.class, DummyProbe.class);
        MokeProbe<String, Number> p = GenerateProbe.quickProbe(testFolder, args);

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
