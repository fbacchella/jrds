package jrds.probe;

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.JrdsTester;
import jrds.ProbeDesc;
import jrds.factories.ProbeDescBuilder;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

public class XmlProbe {
	static final Appender app = new WriterAppender() {
		public void doAppend(LoggingEvent event) {
			System.out.println(event.getLevel() + ": " + event.getMessage());
		}
	};
	static Logger logger = null;
	XPath xpath = XPathFactory.newInstance().newXPath();
	static ProbeDesc pd;


	@BeforeClass static public void configure() throws Exception {
		JrdsTester.configure();

		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(jrds.JrdsLoggerConfiguration.APPENDER).setLevel(Level.INFO);
		Logger.getLogger("org.apache.commons.digester.Digester").setLevel(Level.INFO);
		Logger.getLogger("jrds.probe.HttpXml").setLevel(Level.TRACE);
		logger = Logger.getLogger(XmlProbe.class);
		logger.setLevel(Level.TRACE);

		JrdsTester.prepareXml();

		ProbeDescBuilder builder = new ProbeDescBuilder();
		pd = builder.makeProbeDesc(new JrdsNode(JrdsTester.parseRessource("httpxmlprobedesc.xml")));
	}

	@Test public void findUptime() throws Exception {
		HttpXml p  = new  HttpXml() {
			@Override
			public String getName() {
				return "Moke";
			}
		};
		p.setPd(pd);
		double l;
		Reader uptimeXml;

		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element />");
		l = p.findUptime(JrdsTester.dbuilder.parse(new InputSource(uptimeXml)));
		Assert.assertEquals(0, l, 0.0001);

		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element uptime=\"1.125\" />");
		l = p.findUptime(JrdsTester.dbuilder.parse(new InputSource(uptimeXml)));
		Assert.assertEquals(1.125, l, 0.0001);

		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element />");
		l = p.findUptime(JrdsTester.dbuilder.parse(new InputSource(uptimeXml)));
		Assert.assertEquals(0, l, 0.0001);


		pd.addSpecific("upTimePath", "A/element/@uptime");
		uptimeXml = new StringReader("<?xml version=\"1.0\" ?><element />");
		l = p.findUptime(JrdsTester.dbuilder.parse(new InputSource(uptimeXml)));
		Assert.assertEquals(0, l, 0.0001);
	}

	@Test public void parseXml() throws Exception {
		URL url = this.getClass().getResource("/ressources/xmldata.xml");
		logger.trace("/ressources/xmldata.xml" + " " +url);
		HttpXml p  = new jrds.probe.HttpXml(url) {
			@Override
			public String getName() {
				return "Moke";
			}
		};

		/*Map<String, Object> dsMap = new HashMap<String, Object>();
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
		pd.add(dsMap);*/
		p.setPd(pd);
		Map<?, ?> vars = p.getNewSampleValues();
		Assert.assertEquals(new Double(1.0), vars.get("/jrdsstats/stat[@key='a']/@value"));
		Assert.assertNull(vars.get("/jrdsstats/stat[@key='b']/@value"));
		logger.trace(vars);
	}
}
