package jrds.probe;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.starter.HostStarter;
import jrds.starter.XmlProvider;

public class XmlProbeTest {

    XPath xpath = XPathFactory.newInstance().newXPath();
    static ProbeDesc<String> pd;

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml(false);

        pd = jrds.configuration.GeneratorHelper.getProbeDesc(Tools.parseRessource("httpxmlprobedesc.xml"));
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Probe.HttpXml", "jrds.Probe.HttpProbe", "jrds.starter.XmlProvider");
    }

    @Test
    public void findUptime() throws Exception {
        HttpXml p = new HttpXml() {
            @Override
            public String getName() {
                return "Moke";
            }
        };
        HostStarter host = new HostStarter(new HostInfo("moke"));
        p.setHost(host);
        p.setPd(pd);
        host.registerStarter(new XmlProvider());
        long l;
        String uptimeXml;

        p.find(XmlProvider.class).start();
        uptimeXml = "<?xml version=\"1.0\" ?><element />";
        l = p.findUptime(p.find(XmlProvider.class), Tools.parseString(uptimeXml));
        Assert.assertEquals(0, l, 0.0001);
        p.find(XmlProvider.class).stop();

        p.find(XmlProvider.class).start();
        uptimeXml = "<?xml version=\"1.0\" ?><element uptime=\"1125\" />";
        l = p.findUptime(p.find(XmlProvider.class), Tools.parseString(uptimeXml));
        Assert.assertEquals((long) 1125, l);
        p.find(XmlProvider.class).stop();

        p.find(XmlProvider.class).start();
        uptimeXml = "<?xml version=\"1.0\" ?><element />";
        l = p.findUptime(p.find(XmlProvider.class), Tools.parseString(uptimeXml));
        Assert.assertEquals((long) 0, l);
        p.find(XmlProvider.class).stop();

        pd.addSpecific("upTimePath", "A/element/@uptime");
        p.find(XmlProvider.class).start();
        uptimeXml = "<?xml version=\"1.0\" ?><element />";
        l = p.findUptime(p.find(XmlProvider.class), Tools.parseString(uptimeXml));
        Assert.assertEquals((long) 0, l);
        p.find(XmlProvider.class).stop();
    }

    @Test
    public void manageArgs() {
        URL url = getClass().getResource("/ressources/xmldata.xml");
        List<Object> args = new ArrayList<Object>(1);
        args.add("a");
        args.add("/jrdsstats/stat[@key='a']/@value");
        HttpXml p = new jrds.probe.HttpXml() {
            @Override
            public String getName() {
                return "Moke";
            }
        };
        HostStarter host = new HostStarter(new HostInfo("moke"));
        p.setHost(host);
        p.setPd(pd);
        p.configure(url, args);
        Map<String, String> keys = p.getCollectMapping();
        logger.trace("Collect keys: " + p.getCollectMapping());
        logger.trace("Collect strings: " + pd.getCollectMapping());
        Assert.assertTrue(keys.containsKey("/jrdsstats/stat[@key='%1$s']/@value"));
        Assert.assertTrue(keys.containsKey("/jrdsstats/stat[@key='b']/@value"));
        Assert.assertTrue(keys.containsKey("c"));
    }

    // @Test
    // public void parseXml() throws Exception {
    // URL url = this.getClass().getResource("/ressources/xmldata.xml");
    // List<Object> args = new ArrayList<Object>(1);
    // args.add("a");
    // args.add("/jrdsstats/stat[@key='d']/@value");
    // HttpXml p = new jrds.probe.HttpXml() {
    // @Override
    // public String getName() {
    // return "Moke";
    // }
    // };
    // RdsHost h = new RdsHost("localhost");
    // p.setHost(h);
    // p.setPd(pd);
    // p.getHost().registerStarter(new XmlProvider());
    // p.getHost().registerStarter(new HttpClientStarter());
    //
    // p.configure(url, args);
    // h.startCollect();
    // p.startCollect();
    // Map<?, ?> vars = p.getNewSampleValues();
    // p.stopCollect();
    // h.stopCollect();
    //
    // logger.trace("vars: " + vars);
    // logger.trace("Collect keys: " + p.getCollectMapping());
    // logger.trace("Collect strings: " + pd.getCollectStrings());
    //
    // Assert.assertEquals(new Double(1.0),
    // vars.get("/jrdsstats/stat[@key='a']/@value"));
    // Assert.assertEquals(Double.NaN,
    // vars.get("/jrdsstats/stat[@key='b']/@value"));
    // Assert.assertEquals(new Double(3.5),
    // vars.get("/jrdsstats/stat[@key='d']/@value"));
    // }
}
