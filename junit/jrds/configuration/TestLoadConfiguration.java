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
import jrds.StoreOpener;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.mockobjects.MokeProbe;
import jrds.mockobjects.MokeProbeFactory;
import jrds.snmp.SnmpStarter;
import jrds.starter.ChainedProperties;
import jrds.starter.StarterNode;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestLoadConfiguration {
	static final private Logger logger = Logger.getLogger(TestLoadConfiguration.class);

	static final private String propertiesXmlString = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
		"<!DOCTYPE properties PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
		"<properties>" +
		"<entry key=\"a\">1</entry>" +
		"<entry key=\"b\">2</entry>" +
		"</properties>";

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
	static ConfigObjectFactory conf;
	static PropertiesManager pm = new PropertiesManager();

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.prepareXml();
		Loader l = new Loader();
		dbuilder = l.dbuilder;
		File descdir = new File("desc");
		l.importUrl(descdir.toURI().toURL());

		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.update();
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, null);

		conf = new ConfigObjectFactory(pm);
		conf.setLoader(l);
		conf.setGraphDescMap();
		conf.setProbeDescMap();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories", "jrds.Probe.DummyProbe"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}

	@Test
	public void testFilter() throws Exception {
		JrdsNode d = new JrdsNode(Tools.parseRessource("view1.xml"));

		FilterBuilder fb = new FilterBuilder();
		fb.setPm(pm);
		Filter f = fb.makeFilter(d);
		Assert.assertEquals("Test view 1",f.getName());
	}

	@Test
	public void testProbe2() throws Exception {
		Document d = Tools.parseString(goodProbeXml2);
		JrdsNode pnode = new JrdsNode(d);

		HostBuilder hb = new HostBuilder();
		hb.setProbeFactory(new MokeProbeFactory());
        hb.setPm(pm);
		
		RdsHost host = new RdsHost();
		host.setHostDir(pm.rrddir);
		host.setName("testProbe2");

		Probe<?,?> p = hb.makeProbe(pnode.getChild(CompiledXPath.get("/probe")), host, new StarterNode() {});
		jrds.Util.serialize(p.dumpAsXml(), System.out, null, null);
		Assert.assertNotNull(p);
		Assert.assertEquals(host.getName() + "/" + p.getName() , p.toString());
	}

	@Test
	public void testDsreplace() throws Exception {
		JrdsNode d = new JrdsNode(Tools.parseRessource("dsoverride.xml"));
		JrdsNode pnode = new JrdsNode(d);

		HostBuilder hb = new HostBuilder();
		ProbeFactory pf =  new MokeProbeFactory();
        hb.setProbeFactory(pf);
        hb.setPm(pm);
		
		RdsHost host = new RdsHost();
		host.setHostDir(pm.rrddir);
		host.setName("testDsreplace");

		Probe<?,?> p = hb.makeProbe(pnode.getChild(CompiledXPath.get("/host/probe")), host, new StarterNode() {});
		ProbeDesc pd = p.getPd();
		Assert.assertNotNull(pd);
		Assert.assertEquals(1 , pd.getSize());
		Assert.assertNotSame(pf.getProbeDesc(pd.getName()) , pd.getSize());

	}

	@Test
	public void testMacroLoad() throws Exception {
		Document d = Tools.parseString(goodMacroXml);

		MacroBuilder b = new MacroBuilder();
		JrdsNode jn = new JrdsNode(d);

		Macro m = b.makeMacro(jn);
		int macroProbesNumber = m.getDf().getChildNodes().getLength();
		Assert.assertEquals("macrodef", m.getName());
		Assert.assertEquals("Macro$macrodef", m.toString());
		Assert.assertEquals(1, macroProbesNumber);
		Assert.assertEquals(2, m.getDf().getChildNodes().item(0).getChildNodes().getLength());
	}
	
	@Test
	public void testMacroFill() throws Exception {
		Document d = Tools.parseString(goodMacroXml);

		MacroBuilder b = new MacroBuilder();
		JrdsNode jn = new JrdsNode(d);

		Macro m = b.makeMacro(jn);
		
		Map<String, Macro> macroMap = new HashMap<String, Macro>();
		macroMap.put(m.getName(), m);

		Document hostdoc = Tools.parseString(goodHostXml);
		
		String macroString = "<macro name=\"macrodef\" ></macro>";
		Tools.appendString(hostdoc.getDocumentElement(), macroString);
		jrds.Util.serialize(hostdoc, System.out, null, null);
		System.out.println();
		
		HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
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
		Document d = Tools.parseString(goodMacroXml);
		String tagname = "mytag";
		
		Tools.appendString(d.getDocumentElement(), "<tag>" +tagname + "</tag>");
		Tools.appendString(d.getDocumentElement(), "<snmp community=\"public\" version = \"2\" />");

		MacroBuilder b = new MacroBuilder();
		JrdsNode jn = new JrdsNode(d);

		Macro m = b.makeMacro(jn);
		
		Map<String, Macro> macroMap = new HashMap<String, Macro>();
		macroMap.put(m.getName(), m);

		Document hostdoc = Tools.parseString(goodHostXml);

		Tools.appendString(hostdoc.getDocumentElement(), "<macro name=\"macrodef\" />");

		HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		Assert.assertTrue("tag not found", host.getTags().contains(tagname));
		Assert.assertEquals("SNMP starter not found", "snmp:udp://myhost:161", host.find(SnmpStarter.class).toString());
	}

	@Test
	public void testMacroFillwithProps() throws Exception {
		Document d = Tools.parseString(goodMacroXml);

		MacroBuilder b = new MacroBuilder();
		JrdsNode jn = new JrdsNode(d);

		Macro m = b.makeMacro(jn);
		
		Map<String, Macro> macroMap = new HashMap<String, Macro>();
		macroMap.put(m.getName(), m);

		Document hostdoc = Tools.parseString(goodHostXml);

		Tools.appendString(Tools.appendString(Tools.appendString(hostdoc.getDocumentElement(), "<macro name=\"macrodef\" />"), "<properties />"), "<entry key=\"a\" >bidule</entry>");
		jrds.Util.serialize(hostdoc, System.out, null, null);
		System.out.println();

		HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());

		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
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
		JrdsNode hostNode = new JrdsNode(Tools.parseRessource("goodhost1.xml"));

		String tagname = "mytag";
		Tools.appendString(hostNode.getChild(CompiledXPath.get("/host")), "<tag>" +tagname + "</tag>");
		Tools.appendString(hostNode.getChild(CompiledXPath.get("/host")), "<snmp community=\"public\" version = \"2\" />");

		Map<String, JrdsNode> hostDescMap = new HashMap<String, JrdsNode>();
		hostDescMap.put("name", hostNode);
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
		Assert.assertEquals("SNMP starter not found", "snmp:udp://myhost:161", h.find(SnmpStarter.class).toString());
		logger.trace(h.find(SnmpStarter.class));
	}

	@Test
	public void testProperties() throws Exception {
		JrdsNode pnode = new JrdsNode(Tools.parseString(propertiesXmlString));

		HostBuilder hb = new HostBuilder();
        hb.setPm(pm);

		Map<String, String> props = hb.makeProperties(pnode);
		Assert.assertEquals(2, props.size());
		Assert.assertNotNull(props.get("a"));
		Assert.assertNotNull(props.get("b"));
	}
	
	@Test
	public void testTab() throws Exception {
		JrdsNode tabNode = new JrdsNode(Tools.parseRessource("goodtab.xml"));
		
		TabBuilder tb = new TabBuilder();
		Tab tab = tb.build(tabNode);
		
		Assert.assertEquals("Tab name not set", "goodtab", tab.getName());
	}

}
