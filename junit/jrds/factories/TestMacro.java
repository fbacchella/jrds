package jrds.factories;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Macro;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.mockobjects.MokeProbe;
import jrds.mockobjects.MokeProbeFactory;
import jrds.starter.Starter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class TestMacro {
	static final private Logger logger = Logger.getLogger(TestMacro.class);

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
		"<macro name=\"macrodef\" />" +
		"</host>";

	static DocumentBuilder dbuilder;
	static ConfigObjectFactory conf;
	static PropertiesManager pm = new PropertiesManager();

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.prepareXml(false);
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
		Tools.setLevel(new String[] {"jrds.starter.ChainedProperties"}, Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}
	
	static Macro doMacro(Document d, String name) {
		DocumentFragment df = d.createDocumentFragment();
		df.appendChild(d.removeChild(d.getDocumentElement()));
		
		Macro m = new Macro();
		m.setDf(df);
		m.setName(name);
		return m;
	}
	
	static HostBuilder getBuilder(Macro... macros) {
		HostBuilder hb = new HostBuilder();
		hb.setProperty(ObjectBuilder.properties.PM, pm);
		Map<String, Macro> mmap = new HashMap<String, Macro>(macros.length);
		for(Macro m: macros) {
			mmap.put(m.getName(), m);
		}
		hb.setProperty(ObjectBuilder.properties.MACRO, mmap);
		hb.setProperty(ObjectBuilder.properties.PROBEFACTORY, new MokeProbeFactory());
		return hb;
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
		Macro m = doMacro(d, "macrodef");
		
		Document hostdoc = Tools.parseString(goodHostXml);

		HostBuilder hb = getBuilder(m);

		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
		logger.debug("probes:" + host.getProbes());
		Collection<Probe<?,?>> probes = host.getProbes();
		Collection<String> probesName = new ArrayList<String>(probes.size());
		for(Probe<?,?> p: probes) {
			probesName.add(p.toString());
		}
		logger.trace(probesName);
		Assert.assertTrue("MacroProbe1 not found", probesName.contains("myhost/MacroProbe1"));
		Assert.assertTrue("MacroProbe2 not found", probesName.contains("myhost/MacroProbe2"));
	}

	@Test
	public void testMacroStarter() throws Exception {
		Document d = Tools.parseString(goodMacroXml);
		//Tools.appendString(d.getDocumentElement(), "<snmp community=\"public\" version=\"2\" />");
		//Tools.addElement(d, "snmp", "community=public", "version=2");
		//Tools.JrdsElement je = new Tools.JrdsElement(d);
		//je.addElement("snmp", "community=public", "version=2");
		new Tools.JrdsElement(d).addElement("snmp", "community=public", "version=2");

		Macro m = doMacro(d, "macrodef");
		
		HostBuilder hb = getBuilder(m);

		Document hostdoc = Tools.parseString(goodHostXml);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
		Starter s = host.find(jrds.snmp.SnmpStarter.class);
		
		Assert.assertNotNull("SNMP starter not found", s);
		Assert.assertEquals("Starter not found", jrds.snmp.SnmpStarter.class, s.getKey());
	}
	
	@Test
	public void testMacroTag() throws Exception {
		Document d = Tools.parseString(goodMacroXml);
		String tagname = "mytag";
		new Tools.JrdsElement(d).addElement("tag").setTextContent(tagname);

		Macro m = doMacro(d, "macrodef");
		HostBuilder hb = getBuilder(m);

		Document hostdoc = Tools.parseString(goodHostXml);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		Assert.assertTrue("tag not found", host.getTags().contains(tagname));
	}

	@Test
	public void testMacroFillwithProps1() throws Exception {
		Document d = Tools.parseString(goodMacroXml);
		new Tools.JrdsElement(d).addElement("entry", "key=a").setTextContent("bidule");
		jrds.Util.serialize(d, System.out, null, null);
		Macro m = doMacro(d, "macrodef");

		Document hostdoc = Tools.parseString(goodHostXml);
		new Tools.JrdsElement(hostdoc).addElement("probe", "type=MacroProbe3").addElement("arg", "type=String", "value=${a}");

		HostBuilder hb = getBuilder(m);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
		Collection<Probe<?,?>> probes = host.getProbes();
		boolean found = false;
		for(Probe<?,?> p: probes) {
			if("myhost/MacroProbe3".equals(p.toString()) ) {
				MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
				logger.trace("Args:" + mp.getArgs());
				Assert.assertFalse(mp.getArgs().contains("bidule"));
				found = true;
			}
		}
		Assert.assertTrue("macro probe with properties not found", found);
	}

	@Test
	public void testMacroFillwithProps2() throws Exception {
		Document d = Tools.parseString(goodMacroXml);
		new Tools.JrdsElement(d).addElement("probe", "type=MacroProbe3").addElement("arg", "type=String", "value=${a}");
		//Tools.appendString(Tools.appendString(d.getDocumentElement(), "<probe type = \"MacroProbe3\" />"), "<arg type=\"String\" value=\"${a}\" />");
		Macro m = doMacro(d, "macrodef");

		Document hostdoc = Tools.parseString(goodHostXml);
		new Tools.JrdsElement(hostdoc).addElement("properties").addElement("entry", "key=a").setTextContent("bidule");
		//Tools.appendString(Tools.appendString(hostdoc.getDocumentElement(), "<properties />"), "<entry key=\"a\" >bidule</entry>");

		HostBuilder hb = getBuilder(m);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
		Collection<Probe<?,?>> probes = host.getProbes();
		boolean found = false;
		for(Probe<?,?> p: probes) {
			if("myhost/MacroProbe3".equals(p.toString()) ) {
				MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
				logger.trace("Args:" + mp.getArgs());
				Assert.assertTrue(mp.getArgs().contains("bidule"));
				found = true;
			}
		}
		Assert.assertTrue("macro probe with properties not found", found);
	}
	
	@Test
	public void testMacroFillwithProps3() throws Exception {
		Document d = Tools.parseString(goodMacroXml);
		new Tools.JrdsElement(d).addElement("probe", "type=MacroProbe3").addElement("arg", "type=String", "value=${a}");
		Macro m = doMacro(d, "macrodef");

		Document hostdoc = Tools.parseString(goodHostXml);
		JrdsNode jhostdoc = new JrdsNode(hostdoc);
		JrdsNode macronode = jhostdoc.getChild(CompiledXPath.get("/host/macro"));
		new Tools.JrdsElement(macronode).addElement("properties").addElement("entry", "key=a").setTextContent("bidule");

		HostBuilder hb = getBuilder(m);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));
		
		Collection<Probe<?,?>> probes = host.getProbes();
		boolean found = false;
		for(Probe<?,?> p: probes) {
			if("myhost/MacroProbe3".equals(p.toString()) ) {
				MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
				logger.trace("Args:" + mp.getArgs());
				Assert.assertTrue(mp.getArgs().contains("bidule"));
				found = true;
			}
		}
		Assert.assertTrue("macro probe with properties not found", found);
	}
	
	@Test
	public void testCollection() throws Exception {
		Document d = Tools.parseString(goodMacroXml);
		Tools.appendString(Tools.appendString(Tools.appendString(d.getDocumentElement(), "<for var=\"a\" collection=\"c\"/>"),"<probe type = \"MacroProbe3\" />"), "<arg type=\"String\" value=\"${a}\" />");
		Macro m = doMacro(d, "macrodef");

		Document hostdoc = Tools.parseString(goodHostXml);
		Tools.appendString(Tools.appendString(hostdoc.getDocumentElement(), "<collection name=\"c\"/>"), "<element>bidule</element>");

		HostBuilder hb = getBuilder(m);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));

		Collection<Probe<?,?>> probes = host.getProbes();
		boolean found = false;
		for(Probe<?,?> p: probes) {
			if("myhost/MacroProbe3".equals(p.toString()) ) {
				MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
				logger.trace("Args:" + mp.getArgs());
				Assert.assertTrue(mp.getArgs().contains("bidule"));
				found = true;
			}
		}
		Assert.assertTrue("collection not found", found);
	}
	
	@Test
	public void testRecursive() throws Exception {
		Document d1 = Tools.parseString(goodMacroXml);
		Tools.appendString(d1.getDocumentElement(), "<macro name=\"macrodef2\" />");
		Macro m1 = doMacro(d1, "macrodef");

		Document d2 = Tools.parseString(goodMacroXml);
		Tools.appendString(Tools.appendString(d2.getDocumentElement(), "<probe type = \"MacroProbe3\" />"), "<arg type=\"String\" value=\"bidule\" />");
		Macro m2 = doMacro(d2, "macrodef2");

		HostBuilder hb = getBuilder(m1, m2);
		Document hostdoc = Tools.parseString(goodHostXml);
		RdsHost host = hb.makeRdsHost(new JrdsNode(hostdoc));

		Collection<Probe<?,?>> probes = host.getProbes();
		boolean found = false;
		for(Probe<?,?> p: probes) {
			if("myhost/MacroProbe3".equals(p.toString()) ) {
				MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
				logger.trace("Args:" + mp.getArgs());
				Assert.assertTrue(mp.getArgs().contains("bidule"));
				found = true;
			}
		}
		Assert.assertTrue("macro not recursive", found);
	}
}
