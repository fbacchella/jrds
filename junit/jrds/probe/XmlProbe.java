package jrds.probe;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import jrds.Tools;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.factories.ObjectBuilder;
import jrds.factories.ProbeDescBuilder;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class XmlProbe {
	static Logger logger = Logger.getLogger(XmlProbe.class);
	XPath xpath = XPathFactory.newInstance().newXPath();
	static ProbeDesc pd;


	@BeforeClass static public void configure() throws Exception {
		Tools.configure();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.probe.HttpXml", "jrds.probe.HttpProbe", "jrds.XmlProvider"}, logger.getLevel());
		Tools.prepareXml();

		ProbeDescBuilder builder = new ProbeDescBuilder();
		builder.setProperty(ObjectBuilder.properties.PM, new PropertiesManager());
		pd = builder.makeProbeDesc(new JrdsNode(Tools.parseRessource("httpxmlprobedesc.xml")));
	}

	@Test public void findUptime() throws Exception {
		HttpXml p  = new  HttpXml() {
			@Override
			public String getName() {
				return "Moke";
			}
		};
		p.setHost(new RdsHost("moke"));
		p.setPd(pd);
		p.xmlstarter.start();
		long l;
		String uptimeXml;

		p.xmlstarter.start();
		uptimeXml = new String("<?xml version=\"1.0\" ?><element />");
		l = p.findUptime(Tools.parseString(uptimeXml));
		Assert.assertEquals(0, l, 0.0001);
		p.xmlstarter.stop();

		p.xmlstarter.start();
		uptimeXml = new String("<?xml version=\"1.0\" ?><element uptime=\"1125\" />");
		l = p.findUptime(Tools.parseString(uptimeXml));
		Assert.assertEquals((long) 1125, l);
		p.xmlstarter.stop();

		p.xmlstarter.start();
		uptimeXml = new String("<?xml version=\"1.0\" ?><element />");
		l = p.findUptime(Tools.parseString(uptimeXml));
		Assert.assertEquals((long)0, l);
		p.xmlstarter.stop();

		pd.addSpecific("upTimePath", "A/element/@uptime");
		p.xmlstarter.start();
		uptimeXml = new String("<?xml version=\"1.0\" ?><element />");
		l = p.findUptime(Tools.parseString(uptimeXml));
		Assert.assertEquals((long)0, l);
		p.xmlstarter.stop();
	}

	@Test
	public void manageArgs() throws Exception {
		URL url = getClass().getResource("/ressources/xmldata.xml");
		List<Object> args = new ArrayList<Object>(1);
		args.add("a");
		args.add("/jrdsstats/stat[@key='a']/@value");
		HttpXml p  = new jrds.probe.HttpXml() {
			@Override
			public String getName() {
				return "Moke";
			}
		};
		p.setHost(new RdsHost("moke"));
		p.setPd(pd);
		p.configure(url, args);
		Map<String, String> keys = p.getCollectMapping();
		logger.trace("Collect keys: " + p.getCollectMapping());
		logger.trace("Collect strings: " + pd.getCollectStrings());
		Assert.assertTrue(keys.containsKey("/jrdsstats/stat[@key='a']/@value"));
		Assert.assertTrue(keys.containsKey("/jrdsstats/stat[@key='b']/@value"));
		Assert.assertTrue(keys.containsKey("c"));
	}

	@Test //(expected = java.lang.NullPointerException.class)
	public void parseXml() throws Exception {
		URL url = this.getClass().getResource("/ressources/xmldata.xml");
		List<Object> args = new ArrayList<Object>(1);
		args.add("a");
		args.add("/jrdsstats/stat[@key='d']/@value");
		HttpXml p  = new jrds.probe.HttpXml() {
			@Override
			public String getName() {
				return "Moke";
			}
		};
		RdsHost h = new RdsHost("localhost");
		p.setHost(h);
		p.setPd(pd);

		p.configure(url, args);
		p.getStarters().startCollect();
		h.getStarters().startCollect();
		Map<?, ?> vars = p.getNewSampleValues();
		p.getStarters().stopCollect();
		h.getStarters().stopCollect();

		logger.trace("vars: " + vars);
		logger.trace("Collect keys: " + p.getCollectMapping());
		logger.trace("Collect strings: " + pd.getCollectStrings());

		Assert.assertEquals(new Double(1.0), vars.get("/jrdsstats/stat[@key='a']/@value"));
		Assert.assertNull(vars.get("/jrdsstats/stat[@key='b']/@value"));
		Assert.assertEquals(new Double(3.5), vars.get("/jrdsstats/stat[@key='d']/@value"));


	}
}
