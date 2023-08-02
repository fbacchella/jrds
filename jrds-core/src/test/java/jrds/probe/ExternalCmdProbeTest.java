package jrds.probe;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.starter.HostStarter;
import jrds.starter.Resolver;
import jrds.starter.Timer;
import jrds.store.RrdDbStoreFactory;

public class ExternalCmdProbeTest {

    static private class DummyExternalCmdProbe extends ExternalCmdProbe {

        @Override
        public Map<String, Number> filterValues(Map<String, Number> valuesList) {
            Assert.assertEquals(1.0, valuesList.get("ds1").doubleValue(), 0.0001);
            Assert.assertEquals(2.0, valuesList.get("ds2").doubleValue(), 0.0001);
            return valuesList;
        }

    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, ExternalCmdProbeTest.class.getCanonicalName(), "jrds.Probe");
        logger.debug("starting");
    }


    @Test
    public void test1() throws InvocationTargetException, IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "path=/usr/bin:/bin");

        HostStarter webserver = getHostStarter(pm);
        DummyExternalCmdProbe p = new DummyExternalCmdProbe();
        ProbeDesc<String> pd = new ProbeDesc<>();
        pd.addSpecific("command", "echo");
        pd.addSpecific("arguments", "N:1:2");
        pd.setProbeClass((Class<? extends Probe<String, ?>>) p.getClass());
        pd.add("ds1", DsType.ABSOLUTE);
        pd.add("ds2", DsType.ABSOLUTE);
        pd.setProbeName("DummyExternalCmdProbe");
        pd.setName("DummyExternalCmdProbe");
        p.setMainStore(new RrdDbStoreFactory(), Collections.emptyMap());
        p.setHost(webserver);
        p.setPd(pd);
        p.setName("test");
        p.readProperties(pm);
        Assert.assertTrue(p.configure());
        p.checkStore();
        p.collect();
    }

    private HostStarter getHostStarter(PropertiesManager pm) {
        HostStarter localhost = new HostStarter(new HostInfo("localhost"));
        Timer t = Tools.getDefaultTimer();
        localhost.setParent(t);
        localhost.getHost().setHostDir(testFolder.getRoot());
        t.configureStarters(pm);
        localhost.find(Resolver.class).doStart();
        t.startCollect();
        localhost.startCollect();
        return localhost;
    }

}
