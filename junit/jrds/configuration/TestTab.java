package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestTab {
	static final private Logger logger = Logger.getLogger(TestTab.class);

	static DocumentBuilder dbuilder;
	static ConfigObjectFactory conf;
	static PropertiesManager pm = new PropertiesManager();
	static HostsList hl = new HostsList();

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.prepareXml(false);
//		Loader l = new Loader();
//		dbuilder = l.dbuilder;
//		l.importUrl(Tools.pathToUrl("desc"));

		pm.setProperty("configdir", "tmp/conf");
		pm.setProperty("rrddir", "tmp");
		pm.setProperty("security", "true");
		pm.setProperty("strictparsing", "true");
		pm.update();

		conf = new ConfigObjectFactory(pm);
		
		hl.configure(pm);

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}

	@Test
	public void testLoad() throws Exception {
		Document d = Tools.parseRessource("goodtab.xml");

		TabBuilder tb = new TabBuilder();
		tb.setPm(pm);

		Tab tab = tb.build(new JrdsNode(d));
		tab.setHostlist(hl);
        Assert.assertEquals("goodtab", tab.getName());
		Assert.assertNotNull("No graph tree generated", tab.getGraphTree());
	}

}
