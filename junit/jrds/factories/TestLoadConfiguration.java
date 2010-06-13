package jrds.factories;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Filter;
import jrds.Macro;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.mockobjects.MokeProbeFactory;
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
		"<tag>mytag</tag>" +
		"<probe type = \"MacroProbe1\">" +
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
		"<snmp community=\"public\" version = \"2\" />" +
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
		l.importUrl(new URL("file:desc"));

		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.update();

		conf = new ConfigObjectFactory(pm);
		conf.setGraphDescMap(l.getRepository(Loader.ConfigType.GRAPHDESC));
		conf.setProbeDescMap(l.getRepository(Loader.ConfigType.PROBEDESC));

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}

	@Test
	public void testFilter() throws Exception {
		JrdsNode d = new JrdsNode(Tools.parseRessource("view1.xml"));

		FilterBuilder fb = new FilterBuilder();
		Filter f = fb.makeFilter(d);
		Assert.assertEquals("Test view 1",f.getName());
	}

	@Test(expected = java.lang.NullPointerException.class)
	public void testProbe2() throws Exception {
		Document d = Tools.parseString(goodProbeXml2);
		JrdsNode pnode = new JrdsNode(d);

		HostBuilder hb = new HostBuilder();
		hb.setProperty(ObjectBuilder.properties.PROBEFACTORY, new MokeProbeFactory());
		hb.setProperty(ObjectBuilder.properties.PM, pm);

		Probe<?,?> p = hb.makeProbe(pnode.getChild(CompiledXPath.get("//probe")), new RdsHost(), new StarterNode() {});
		//		Probe p = conf.makeProbe(pnode.getChild(CompiledXPath.get("//probe")));
		Assert.assertNotNull(p);
		Assert.assertEquals("<empty>/fs-_", p.toString());
	}

	@Test
	public void testMacroLoad() throws Exception {
		Document d = Tools.parseString(goodMacroXml);

		MacroBuilder b = new MacroBuilder();
		JrdsNode jn = new JrdsNode(d);

		Macro m = b.makeMacro(jn);
		int macroProbesNumber = m.getDf().getChildNodes().getLength();
		Assert.assertEquals("macrodef", m.getName());
		Assert.assertEquals(1, macroProbesNumber);
		Assert.assertEquals(3, m.getDf().getChildNodes().item(0).getChildNodes().getLength());
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
		
		Map<String, String> attr = new HashMap<String, String>(1);
		attr.put("name", "macrodef");
		Tools.appendElement(hostdoc.getDocumentElement(), "macro", attr);
		jrds.Util.serialize(hostdoc, System.out, null, null);
		System.out.println();
		
		HostBuilder hb = new HostBuilder();
		hb.setProperty(ObjectBuilder.properties.PM, pm);
		hb.setProperty(ObjectBuilder.properties.MACRO, macroMap);
		hb.setProperty(ObjectBuilder.properties.PROBEFACTORY, new MokeProbeFactory());

		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
		logger.debug("probes:" + host.getProbes());
		Collection<Probe<?,?>> probes = host.getProbes();
		Collection<String> probesName = new ArrayList<String>(probes.size());
		for(Probe<?,?> p: probes) {
			probesName.add(p.toString());
		}
		Assert.assertTrue("MacroProbe1 found", probesName.contains("myhost/MacroProbe1"));
		Assert.assertTrue("MacroProbe1 found", probesName.contains("myhost/MacroProbe2"));
	}

	@Test
	public void testHost() throws Exception {
		JrdsNode hostNode = new JrdsNode(Tools.parseRessource("goodhost1.xml"));

		Map<String, JrdsNode> hostDescMap = new HashMap<String, JrdsNode>();
		hostDescMap.put("name", hostNode);
		Map<String, RdsHost> hostMap = conf.setHostMap(hostDescMap);
		logger.trace(hostMap);
		RdsHost h = hostMap.get("myhost");
		Assert.assertNotNull(h);
		Assert.assertEquals("myhost", h.getName());
		logger.debug("properties: " + h.find(ChainedProperties.class));
		Collection<Probe<?,?>> probes = h.getProbes();
		Assert.assertEquals(7, probes.size());
		//		HostBuilder hb = new HostBuilder();
		//		hb.setProperty(ObjectBuilder.properties.ARGFACTORY, new ArgFactory());

		//		RdsHost h = hb.makeRdsHost(hostNode);
		//		logger.trace(h.getProbes());
	}

	@Test
	public void TestProperties() throws Exception {
		JrdsNode pnode = new JrdsNode(Tools.parseString(propertiesXmlString));

		HostBuilder hb = new HostBuilder();
		hb.setProperty(ObjectBuilder.properties.PM, pm);

		Map<String, String> props = hb.makeProperties(pnode);
		Assert.assertEquals(2, props.size());
		Assert.assertNotNull(props.get("a"));
		Assert.assertNotNull(props.get("b"));
	}

}
