package jrds.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Filter;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.mockobjects.MokeProbe;
import jrds.mockobjects.MokeProbeFactory;
import jrds.snmp.SnmpConnection;
import jrds.starter.ChainedProperties;
import jrds.starter.StarterNode;

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

    static final private String propertiesXmlString = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<host>" +
                    "<properties>" +
                    "<entry key=\"a\">1</entry>" +
                    "<entry key=\"b\">2</entry>" +
                    "</properties>" +
                    "</host>";

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
                    //		"<snmp community=\"public\" version = \"2\" />" +
                    //		"<tag>mytag</tag>" +
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
    //static ConfigObjectFactory conf;
    //static PropertiesManager pm = new PropertiesManager();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
        Tools.setLevel(logger, Level.TRACE, "jrds", "jrds.configuration", "jrds.Probe.DummyProbe", "jrds.snmp");
        Logger.getLogger("jrds.factories.xml.CompiledXPath").setLevel(Level.INFO);
    }

    private ConfigObjectFactory prepare(PropertiesManager pm) throws IOException {
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.update();
        pm.libspath.clear();
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
        PropertiesManager pm = new PropertiesManager();
        FilterBuilder fb = new FilterBuilder();
        fb.setPm(pm);
        Filter f = fb.makeFilter(d);
        Assert.assertEquals("Test view 1",f.getName());
    }

    @Test
    public void testProbe2() throws Exception {
        JrdsDocument d = Tools.parseString(goodProbeXml2);
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.update();
        HostBuilder hb = new HostBuilder();
        hb.setProbeFactory(new MokeProbeFactory());
        hb.setPm(pm);

        RdsHost host = new RdsHost();
        host.setHostDir(pm.rrddir);
        host.setName("testProbe2");

        Probe<?,?> p = hb.makeProbe(d.getRootElement(), host, new StarterNode() {});
        jrds.Util.serialize(p.dumpAsXml(), System.out, null, null);
        Assert.assertNotNull(p);
        Assert.assertEquals(host.getName() + "/" + p.getName() , p.toString());
    }

    @Test
    public void testDsreplace() throws Exception {
        JrdsDocument d = Tools.parseRessource("dsoverride.xml");
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.update();
        HostBuilder hb = new HostBuilder();
        ProbeFactory pf =  new MokeProbeFactory();
        hb.setProbeFactory(pf);
        hb.setPm(pm);

        RdsHost host = new RdsHost();
        host.setHostDir(pm.rrddir);
        host.setName("testDsreplace");

        Probe<?,?> p = hb.makeProbe(d.getRootElement().getElementbyName("probe"), host, new StarterNode() {});
        ProbeDesc pd = p.getPd();
        Assert.assertNotNull(pd);
        Assert.assertEquals(1 , pd.getSize());
        Assert.assertNotSame(pf.getProbeDesc(pd.getName()) , pd.getSize());

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
        PropertiesManager pm = new PropertiesManager();
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

        RdsHost host = hb.makeRdsHost(hostdoc);

        logger.debug("probes:" + host.getProbes());
        Collection<Probe<?,?>> probes = host.getProbes();
        Collection<String> probesName = new ArrayList<String>(probes.size());
        for(Probe<?,?> p: probes) {
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
        PropertiesManager pm = new PropertiesManager();
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

        RdsHost host = hb.makeRdsHost(hostdoc);
        logger.trace("dns name: " + host.getName());
        Assert.assertTrue("tag not found", host.getTags().contains(tagname));
        Assert.assertEquals("SNMP starter not found", "snmp:udp://myhost:161", host.find(SnmpConnection.class).toString());
    }

    @Test
    public void testMacroFillwithProps() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);

        Map<String, Macro> macroMap = new HashMap<String, Macro>();
        macroMap.put(m.getName(), m);

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);

        JrdsElement je = hostdoc.getRootElement();
        je.addElement("macro", "name=macrodef");
        je.addElement("properties").
        addElement("entry", "key=a").addTextNode("bidule");
        PropertiesManager pm = new PropertiesManager();
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

        RdsHost host = hb.makeRdsHost(hostdoc);

        Collection<Probe<?,?>> probes = host.getProbes();
        boolean found = false;
        for(Probe<?,?> p: probes) {
            if("myhost/MacroProbe1".equals(p.toString()) ) {
                MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
                Assert.assertTrue(mp.getArgs().contains("bidule"));
                found = true;
            }
        }
        Assert.assertTrue("macro probe with properties not found", found);
    }

    @Test
    public void testHost() throws Exception {
        JrdsDocument hostNode = Tools.parseRessource("goodhost1.xml");

        String tagname = "mytag";

        jrds.factories.xml.JrdsElement je = hostNode.getRootElement();
        je.addElement("tag").addTextNode(tagname);
        je.addElement("snmp", "community=public", "version=2");
        PropertiesManager pm = new PropertiesManager();
        Map<String, JrdsDocument> hostDescMap = new HashMap<String, JrdsDocument>();
        hostDescMap.put("name", hostNode);
        ConfigObjectFactory conf = prepare(pm);
        conf.getLoader().setRepository(ConfigType.HOSTS, hostDescMap);
        Map<String, RdsHost> hostMap = conf.setHostMap();
        logger.trace(hostMap);
        RdsHost h = hostMap.get("myhost");
        Assert.assertNotNull(h);
        Assert.assertEquals("myhost", h.getName());
        logger.debug("properties: " + h.find(ChainedProperties.class));
        Collection<Probe<?,?>> probes = h.getProbes();
        Assert.assertEquals(7, probes.size());
        Assert.assertTrue("tag not found", h.getTags().contains(tagname));
        Assert.assertEquals("SNMP starter not found", "snmp:udp://myhost:161", h.find(SnmpConnection.class).toString());
        logger.trace(h.find(SnmpConnection.class));
    }

    @Test
    public void testProperties() throws Exception {
        JrdsDocument pnode = Tools.parseString(propertiesXmlString);
        PropertiesManager pm = new PropertiesManager();
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);

        Map<String, String> props = hb.makeProperties(pnode.getRootElement());
        Assert.assertEquals(2, props.size());
        Assert.assertNotNull(props.get("a"));
        Assert.assertNotNull(props.get("b"));
    }

    @Test
    public void testTab() throws Exception {
        JrdsDocument tabNode = Tools.parseRessource("goodtab.xml");

        TabBuilder tb = new TabBuilder();
        Tab tab = tb.build(tabNode);

        Assert.assertEquals("Tab name not set", "goodtab", tab.getName());
    }

}
