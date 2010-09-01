package jrds.factories;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.HostsList;
import jrds.PropertiesManager;
//import jrds.Tab;
import jrds.Tools;
import jrds.factories.xml.JrdsNode;
import jrds.graphe.Sum;
import jrds.probe.SumProbe;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestTab {
	static final private Logger logger = Logger.getLogger(TestTab.class);

	static final private String goodTabXml =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
		"<!DOCTYPE tab PUBLIC \"-//jrds//DTD Tab//EN\" \"urn:jrds:tab\">" +
		"<tab name=\"tabname\">" +
		"<filter>afilter</filter>" +
		"<graph id=\"graphid\">" +
		"<path>p1</path>" +
		"<path>p2</path>" +
		"</graph>" +
		"</tab>";

	static DocumentBuilder dbuilder;
	static ConfigObjectFactory conf;
	static PropertiesManager pm = new PropertiesManager();

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.prepareXml(false);
		Loader l = new Loader();
		dbuilder = l.dbuilder;
		l.importUrl(Tools.pathToUrl("desc"));

		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.setProperty("security", "true");
		pm.update();

		conf = new ConfigObjectFactory(pm);

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}

	@Test
	public void testLoad() throws Exception {
//		Document d = Tools.parseString(goodTabXml);
//
//		TabBuilder tb = new TabBuilder();
//		tb.setProperty(ObjectBuilder.properties.PM, pm);
//
//		Tab tab = tb.makeTab(new JrdsNode(d));
//		Assert.assertEquals("tabname", tab.getName());
	}

}
