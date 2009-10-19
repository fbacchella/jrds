package jrds.factories;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.ChainedProperties;
import jrds.Filter;
import jrds.Macro;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestLoadConfiguration {
	static final private Logger logger = Logger.getLogger(TestLoadConfiguration.class);

	static final private String propertiesXmlString = 
		"<properties>" +
		"<entry key=\"a\">1</entry>" +
		"<entry key=\"b\">2</entry>" +
		"</properties>";

	static final private String goodProbeXml2 = 
		"<probe type = \"PartitionSpace\">" +
		"<arg type=\"String\" value=\"/\" />" +
		"</probe>";

	static final private String goodMacroXml = 
		"<macrodef name=\"macrodef\">" +
		"<probe type = \"TcpSnmp\">" +
		"</probe>" + 
		"<probe type = \"PartitionSpace\">" +
		"<arg type=\"String\" value=\"/\" />" +
		"</probe>" +
		"</macrodef>";

	static final private String goodHostXml = 
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
		Tools.setLevel(new String[] {"jrds.factories","jrds.Probe"}, logger.getLevel());

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
		Probe<?,?> p = hb.makeProbe(pnode.getChild(CompiledXPath.get("//probe")), new RdsHost());
		//		Probe p = conf.makeProbe(pnode.getChild(CompiledXPath.get("//probe")));
		Assert.assertNotNull(p);
		Assert.assertEquals("<empty>/fs-_", p.toString());
	}

	@Test
	public void testMacro() throws Exception {
		Document d = Tools.parseString(goodMacroXml);

		MacroBuilder b = new MacroBuilder();
		Macro m = b.makeMacro(new JrdsNode(d));
		Assert.assertEquals("macrodef", m.getName());
		logger.debug("Macro: " + m);
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
		logger.debug("properties: " + h.getStarters().find(ChainedProperties.class.getName()));
		Collection<Probe<?,?>> probes = h.getProbes();
		Assert.assertEquals(2, probes.size());
		//		HostBuilder hb = new HostBuilder();
		//		hb.setProperty(ObjectBuilder.properties.ARGFACTORY, new ArgFactory());

		//		RdsHost h = hb.makeRdsHost(hostNode);
		//		logger.trace(h.getProbes());
	}

	@Test(expected = java.lang.NullPointerException.class)
	public void testMacroHost() throws Exception {
		Document macrodoc = Tools.parseString(goodMacroXml);

		MacroBuilder mb = new MacroBuilder();
		mb.setProperty(ObjectBuilder.properties.PM, pm);

		Macro m = mb.makeMacro(new JrdsNode(macrodoc));

		Document hostdoc = Tools.parseString(goodHostXml);
		JrdsNode hostNode = new JrdsNode(hostdoc);

		HostBuilder hb = new HostBuilder();
		hb.setProperty(ObjectBuilder.properties.PM, pm);
		RdsHost h = hb.makeRdsHost(hostNode);
		Assert.assertEquals("myhost",h.getName());

		Map<String, String> properties = Collections.emptyMap();
		m.populate(h, properties);
		logger.trace(h.getProbes());
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
