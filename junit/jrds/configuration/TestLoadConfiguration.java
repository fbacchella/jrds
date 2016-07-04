package jrds.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Filter;
import jrds.HostInfo;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.mockobjects.MokeProbeFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestLoadConfiguration {
    static final private Logger logger = Logger.getLogger(TestLoadConfiguration.class);

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    static final private String goodProbeXml2 = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE probe PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<probe type = \"PartitionSpace\">" +
                    "<arg type=\"String\" value=\"/\" />" +
                    "</probe>";

    static final private String goodMacroXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE macrodef PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<macrodef name=\"macrodef\">" +
                    "<probe type = \"MacroProbe1\">" +
                    "<arg type=\"String\" value=\"${a}\" />" +
                    "</probe>" + 
                    "<probe type = \"MacroProbe2\">" +
                    "<arg type=\"String\" value=\"/\" />" +
                    "</probe>" +
                    "</macrodef>";

    static final private String goodHostXml = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE host PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<host name=\"myhost\">" +
                    "<probe type = \"PartitionSpace\">" +
                    "<arg type=\"String\" value=\"/\" />" +
                    "</probe>" +
                    "<probe type = \"TcpSnmp\">" +
                    "</probe>" +
                    "</host>";

    static DocumentBuilder dbuilder;

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
        Tools.setLevel(logger, Level.TRACE, "jrds", "jrds.configuration", "jrds.Probe.DummyProbe", "jrds.snmp");
        Logger.getLogger("jrds.factories.xml.CompiledXPath").setLevel(Level.INFO);
    }

    private ConfigObjectFactory prepare(PropertiesManager pm) throws IOException {
        File descpath = new File(System.getProperty("user.dir"), "desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());

        ConfigObjectFactory conf = new ConfigObjectFactory(pm);
        conf.setGraphDescMap();
        conf.setProbeDescMap();
        return conf;
    }

    @Test
    public void testFilter() throws Exception {
        JrdsDocument d = Tools.parseRessource("view1.xml");
        PropertiesManager pm = Tools.makePm(testFolder);
        FilterBuilder fb = new FilterBuilder();
        fb.setPm(pm);
        Filter f = fb.makeFilter(d);
        Assert.assertEquals("Test view 1", f.getName());
    }

    @Test
    public void testProbe2() throws Exception {
        JrdsDocument d = Tools.parseString(goodProbeXml2);
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        hb.setProbeFactory(new MokeProbeFactory());
        hb.setPm(pm);
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = new HostInfo("testProbe2");
        host.setHostDir(pm.rrddir);

        Probe<?, ?> p = hb.makeProbe(d.getRootElement(), host, null);
        jrds.Util.serialize(p.dumpAsXml(), System.out, null, null);
        Assert.assertNotNull(p);
        Assert.assertEquals(host.getName() + "/" + p.getName(), p.toString());
    }

    @Test
    public void testDsreplace() throws Exception {
        JrdsDocument d = Tools.parseRessource("dsoverride.xml");
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        ProbeFactory pf = new MokeProbeFactory();
        hb.setProbeFactory(pf);
        hb.setPm(pm);
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = new HostInfo("testDsreplace");
        host.setHostDir(pm.rrddir);

        Probe<?, ?> p = hb.makeProbe(d.getRootElement().getElementbyName("probe"), host, null);
        ProbeDesc pd = p.getPd();
        Assert.assertNotNull(pd);
        Assert.assertEquals(1, pd.getSize());
        Assert.assertNotSame(pf.getProbeDesc(pd.getName()), pd.getSize());

    }

    @Test
    public void testMacroLoad() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);
        int macroProbesNumber = m.getDf().getChildNodes().getLength();
        Assert.assertEquals("macrodef", m.getName());
        Assert.assertEquals("Macro$macrodef", m.toString());
        Assert.assertEquals(1, macroProbesNumber);
        Assert.assertEquals(2, m.getDf().getChildNodes().item(0).getChildNodes().getLength());
    }

    @Test
    public void testMacroFill() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);

        Map<String, Macro> macroMap = new HashMap<String, Macro>();
        macroMap.put(m.getName(), m);

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        hostdoc.setDocumentURI("-//jrds//DTD Graph Description//EN");

        hostdoc.getRootElement().addElement("macro", "name=macrodef");
        jrds.Util.serialize(hostdoc, System.out, null, null);
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = hb.makeHost(hostdoc);

        logger.debug("probes:" + host.getProbes());
        Collection<String> probesName = new ArrayList<String>();
        for(Probe<?, ?> p: host.getProbes()) {
            probesName.add(p.toString());
        }
        Assert.assertTrue("MacroProbe1 not found", probesName.contains("myhost/MacroProbe1"));
        Assert.assertTrue("MacroProbe2 not found", probesName.contains("myhost/MacroProbe2"));
    }

    @Test
    public void testRecursiveMacro() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);
        String tagname = "mytag";

        JrdsElement root = d.getRootElement();
        root.addElement("tag").addTextNode(tagname);
        root.addElement("snmp", "community=public", "version=2");

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);

        Map<String, Macro> macroMap = new HashMap<String, Macro>();
        macroMap.put(m.getName(), m);

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        hostdoc.getRootElement().addElement("macro", "name=macrodef");
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

        HostInfo host = hb.makeHost(hostdoc);
        logger.trace("dns name: " + host.getName());
        Assert.assertTrue("tag not found", host.getTags().contains(tagname));
        // Assert.assertEquals("SNMP starter not found",
        // "snmp:udp://myhost:161", host.find(SnmpConnection.class).toString());
    }

    @Test
    public void testHost() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);
        ConfigObjectFactory conf = prepare(pm);
        Map<String, JrdsDocument> hostDescMap = new HashMap<String, JrdsDocument>();
        conf.getLoader().setRepository(ConfigType.HOSTS, hostDescMap);

        JrdsDocument hostNode = Tools.parseRessource("goodhost1.xml");
        String tagname = "mytag";
        JrdsElement je = hostNode.getRootElement();
        je.addElement("tag").addTextNode(tagname);
        je.addElement("snmp", "community=public", "version=2");
        hostDescMap.put("name", hostNode);
        Map<String, HostInfo> hostMap = conf.setHostMap(Tools.getSimpleTimerMap());

        logger.trace(hostMap);
        HostInfo h = hostMap.get("myhost");
        Assert.assertNotNull(h);
        Assert.assertEquals("myhost", h.getName());
        Collection<Probe<?, ?>> probes = new HashSet<Probe<?, ?>>();
        for(Probe<?, ?> p: h.getProbes())
            probes.add(p);
        Assert.assertEquals(7, h.getNumProbes());
        Assert.assertTrue("tag not found", h.getTags().contains(tagname));
        // Assert.assertEquals("SNMP starter not found",
        // "snmp:udp://myhost:161", h.find(SnmpConnection.class).toString());
        // logger.trace(h.find(SnmpConnection.class));
    }

    @Test
    public void testTab() throws Exception {
        JrdsDocument tabNode = Tools.parseRessource("goodtab.xml");

        TabBuilder tb = new TabBuilder();
        Tab tab = tb.build(tabNode);

        Assert.assertEquals("Tab name not set", "goodtab", tab.getName());
    }

}
