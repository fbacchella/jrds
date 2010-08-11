package jrds.factories;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.Filter;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsNode;
import jrds.webapp.ACL;
import jrds.webapp.RolesACL;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestFilter {
	static final private Logger logger = Logger.getLogger(TestSum.class);

	static final private String goodFilterXml =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
		"<!DOCTYPE filter PUBLIC \"-//jrds//DTD Filter//EN\" \"urn:jrds:filter\">" +
		"<filter>" +
		"<name>filtername</name>" +
		"</filter>";

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
		pm.setProperty("security", "yes");
		pm.update();

		conf = new ConfigObjectFactory(pm);
		conf.setGraphDescMap(l.getRepository(Loader.ConfigType.GRAPHDESC));
		conf.setProbeDescMap(l.getRepository(Loader.ConfigType.PROBEDESC));

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories", "jrds.Filter", "jrds.FilterXml"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}
	
	private Filter doFilter(Document d) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		FilterBuilder sm = new FilterBuilder();
		sm.setProperty(ObjectBuilder.properties.PM, pm);
		Filter sp = sm.makeFilter(new JrdsNode(d));
		
		return sp;
	}
	
	@Test
	public void testLoad() throws Exception {
		Document d = Tools.parseString(goodFilterXml);
		Tools.JrdsElement je = new Tools.JrdsElement(d);
		je.addElement("path").setTextContent("^.*$");
		Filter f = doFilter(d);
		
		Assert.assertEquals("Filter name not match", f.getName(), "filtername");
	}

	@Test
	public void testACL() throws Exception {
		Document d = Tools.parseString(goodFilterXml);
		Tools.JrdsElement je = new Tools.JrdsElement(d);
		je.addElement("role").setTextContent("role1");
		je.addElement("path").setTextContent("^.*$");
		Filter f = doFilter(d);
		
		ACL acl = f.getACL();
		Assert.assertEquals("Not an role ACL", RolesACL.class, acl.getClass());
	}

}
