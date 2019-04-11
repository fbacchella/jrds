package jrds.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import jrds.GraphDesc;
import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.starter.Timer;

public class TestSnmpHostBuilder {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.prepareXml(true);
    }

    @Test
    public void testFullHost() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder, "jrds.snmpdirs=/usr/share/snmp/mibs");
        Tools.findDescs(pm);
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);

        Map<String, GraphDesc> graphDescMap = conf.setGraphDescMap();
        Map<String, ProbeDesc<? extends Object>> probeDescMap = conf.setProbeDescMap();
        ProbeFactory pf = new ProbeFactory(probeDescMap, graphDescMap);

        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setClassLoader(this.getClass().getClassLoader());
        hb.setMacros(new HashMap<String, Macro>(0));
        hb.setProbeFactory(pf);
        Map<String, Timer> timerMap = Tools.getSimpleTimerMap();
        timerMap.put("another", timerMap.get(Timer.DEFAULTNAME));
        hb.setTimers(timerMap);

        JrdsDocument fullhost = Tools.parseRessource("fullhost.xml");

        HostInfo hi = hb.build(fullhost);

        Assert.assertEquals("fqdn.jrds.fr", hi.getDnsName());

        Map<String, Probe<?, ?>> probes = new HashMap<String, Probe<?, ?>>(hi.getNumProbes());

        for(Probe<?, ?> p: hi.getProbes()) {
            String name = p.getQualifiedName();
            probes.put(name, p);
        }
        Assert.assertTrue("myhost/tcp_snmp not found in " + probes.toString(), probes.containsKey("myhost/tcp_snmp"));
        Assert.assertTrue("myhost/fs-_ not found in " + probes.toString(), probes.containsKey("myhost/fs-_"));
        Assert.assertTrue("myhost/fs-_data not found in " + probes.toString(), probes.containsKey("myhost/fs-_data"));
        Assert.assertTrue("myhost/ifx-eth0 not found in " + probes.toString(), probes.containsKey("myhost/ifx-eth0"));
        Assert.assertTrue("myhost/ifx-eth1 not found in " + probes.toString(), probes.containsKey("myhost/ifx-eth1"));
        Assert.assertTrue("myhost/ifx-eth2 not found in " + probes.toString(), probes.containsKey("myhost/ifx-eth2"));
        Assert.assertTrue("myhost/ifx-eth3 not found in " + probes.toString(), probes.containsKey("myhost/ifx-eth3"));
    }

}
