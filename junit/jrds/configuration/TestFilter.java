package jrds.configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.objects.Filter;
import jrds.webapp.ACL;
import jrds.webapp.RolesACL;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFilter {
	static final private Logger logger = Logger.getLogger(TestSum.class);

	static final private String goodFilterXml =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
		"<!DOCTYPE filter PUBLIC \"-//jrds//DTD Filter//EN\" \"urn:jrds:filter\">" +
		"<filter>" +
		"<name>filtername</name>" +
		"</filter>";

	static private ConfigObjectFactory conf;
	static private PropertiesManager pm = new PropertiesManager();

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.prepareXml(false);

		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.setProperty("security", "yes");
		pm.update();

		conf = new ConfigObjectFactory(pm);
		conf.setGraphDescMap();
		conf.setProbeDescMap();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(logger, Level.TRACE, "jrds.factories", "jrds.Filter", "jrds.FilterXml");
		Tools.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
	}
	
	private Filter doFilter(JrdsDocument d) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		FilterBuilder sm = new FilterBuilder();
		sm.setPm(pm);
		Filter sp = sm.makeFilter(d);
		
		return sp;
	}
	
	@Test
	public void testLoad() throws Exception {
		JrdsDocument d = Tools.parseString(goodFilterXml);
		JrdsElement je = d.getRootElement();
		je.addElement("path").setTextContent("^.*$");
		Filter f = doFilter(d);
		
		Assert.assertEquals("Filter name not match", f.getName(), "filtername");
	}

	@Test
	public void testACL() throws Exception {
	    JrdsDocument d = Tools.parseString(goodFilterXml);
		JrdsElement je = d.getRootElement();
		je.addElement("role").setTextContent("role1");
		je.addElement("path").setTextContent("^.*$");
		Filter f = doFilter(d);
		
		ACL acl = f.getACL();
		Assert.assertEquals("Not an role ACL", RolesACL.class, acl.getClass());
	}

}
