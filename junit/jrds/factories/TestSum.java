package jrds.factories;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.HostsList;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.JrdsNode;
import jrds.graphe.Sum;
import jrds.mockobjects.MokeProbe;
import jrds.probe.SumProbe;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestSum {
	static final private Logger logger = Logger.getLogger(TestSum.class);

	static final private String goodSumSXml =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
		"<!DOCTYPE sum PUBLIC \"-//jrds//DTD Sum//EN\" \"urn:jrds:sum\">" +
		"<sum name=\"sumname\">" +
		"</sum>";

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
		Tools.setLevel(new String[] {"jrds.factories", "jrds.probe.SumProbe","jrds.graphe.Sum"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}

	private SumProbe doSumProbe(Document d, HostsList hl) {
		SumBuilder sm = new SumBuilder();
		sm.setProperty(ObjectBuilder.properties.PM, pm);
		SumProbe sp = sm.makeSum(new JrdsNode(d));
		RdsHost host = new RdsHost("SumHost");
		sp.setHost(host);
		hl.addHost(host);
		hl.addProbe(sp);

		Probe<?, ?> mp = new MokeProbe<String, String>();
		hl.addHost(mp.getHost());
		hl.addProbe(mp);
		
		return sp;
	}

	@Test
	public void testLoad() throws Exception {

		Document d = Tools.parseString(goodSumSXml);
		Tools.JrdsElement je = new Tools.JrdsElement(d);
		je.addElement("element", "name=DummyHost/DummyProbe");

		HostsList hl = new HostsList();		
		SumProbe sp = doSumProbe(d, hl);
		Assert.assertEquals("name mismatch", "sumname", "sumname");
		Sum s = (Sum) sp.getGraphList().toArray()[0];
		logger.trace(s);
	}

	@Test
	public void testRoles() throws Exception {
		Document d = Tools.parseString(goodSumSXml);
		Tools.JrdsElement je = new Tools.JrdsElement(d);
		je.addElement("element", "name=DummyHost/DummyProbe");
		je.addElement("role").setTextContent("role1");

		HostsList hl = new HostsList();
		SumProbe sp = doSumProbe(d, hl);

		Sum s = (Sum) sp.getGraphList().toArray()[0];
		Assert.assertTrue("role not found", s.roleAllowed("role1"));
	}

}
