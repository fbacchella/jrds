package jrds.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.ProbeDesc;
import jrds.probe.HttpXml;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.DsType;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlProbe extends jrds.probe.HttpXml {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};
	static Logger logger = null;
	XPath xpath = XPathFactory.newInstance().newXPath();


	@BeforeClass static public void configure() {
		System.getProperties().setProperty("java.awt.headless","true");
		jrds.JrdsLoggerConfiguration.initLog4J();
		app.setName(jrds.JrdsLoggerConfiguration.APPENDER);
		jrds.JrdsLoggerConfiguration.putAppender(app);
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
		Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
		Logger.getLogger("jrds.probe.HttpXml").setLevel(Level.TRACE);
		logger = Logger.getLogger(XmlProbe.class);
		logger.setLevel(Level.TRACE);
	}

	@Test public void findUptime() throws ParserConfigurationException, SAXException, IOException {
		ProbeDesc pd = new ProbeDesc();
		HttpXml p  = new jrds.probe.HttpXml() {
			@Override
			public String getName() {
				return "Moke";
			}
		};
		p.setPd(pd);
		DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		double l;
		Reader uptimeXml;

		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element />");
		//l = p.findUptime(dbuilder.parse(new InputSource(uptimeXml)), xpath);
		//Assert.assertEquals(0, l, 0.0001);

		pd.addSpecific("upTimePath", "/element/@uptime");
		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element uptime=\"1.125\" />");
		//l = p.findUptime(dbuilder.parse(new InputSource(uptimeXml)), xpath);
		//Assert.assertEquals(1.125, l, 0.0001);

		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element />");
		//l = p.findUptime(dbuilder.parse(new InputSource(uptimeXml)), xpath);
		//Assert.assertEquals(0, l, 0.0001);

		
		pd.addSpecific("upTimePath", "A/element/@uptime");
		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element />");
		//l = p.findUptime(dbuilder.parse(new InputSource(uptimeXml)), xpath);
		//Assert.assertEquals(0, l, 0.0001);
	}
	
	@Test public void parseXml() throws ParserConfigurationException, SAXException, IOException {
		String ressource = "/" + this.getClass().getPackage().getName().replace(".", "/") + "/xmldata.xml";
		URL url = this.getClass().getResource(ressource);
		logger.trace(ressource + " " +url);
		HttpXml p  = new jrds.probe.HttpXml(url) {
			@Override
			public String getName() {
				return "Moke";
			}
		};
		ProbeDesc pd = new ProbeDesc();
		Map<String, Object> dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "a");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collectKey", "/jrdsstats/stat[@key='a']/@value");
		pd.add(dsMap);
		dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "b");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collectKey", "/jrdsstats/stat[@key='b']/@value");
		dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "c");
		dsMap.put("dsType", DsType.COUNTER);
		pd.add(dsMap);
		p.setPd(pd);
		Map<?, ?> vars = p.getNewSampleValues();
		Assert.assertEquals(new Double(1.0), vars.get("/jrdsstats/stat[@key='a']/@value"));
		Assert.assertNull(vars.get("/jrdsstats/stat[@key='b']/@value"));
		logger.trace(vars);
	}
}
