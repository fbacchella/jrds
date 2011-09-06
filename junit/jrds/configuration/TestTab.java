package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.PropertiesManager;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTab {
	static final private Logger logger = Logger.getLogger(TestTab.class);

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
