package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.JrdsNode;
import jrds.graphe.Sum;
import jrds.mockobjects.MockGraph;
import jrds.webapp.ACL;
import jrds.webapp.RolesACL;
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
		l.importUrl(Tools.pathToUrl("desc"));

		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.setProperty("security", "true");
		pm.update();

		conf = new ConfigObjectFactory(pm);
		conf.setLoader(l);
		conf.setGraphDescMap();
		conf.setProbeDescMap();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories", "jrds.probe.SumProbe","jrds.graphe.Sum"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
	}

	private Sum doSum(Document d, HostsList hl) throws Exception {
        RdsHost host = new RdsHost("SumHost");

        SumBuilder sm = new SumBuilder();
		sm.setPm(pm);
		Sum sp = sm.makeSum(new JrdsNode(d));
		sp.configure(hl);
		sp.getProbe().setHost(host);
		hl.addHost(host);
		hl.addProbe(sp.getProbe());

        jrds.GraphNode mg = new MockGraph();
        mg.getGraphDesc().add("plot");
        jrds.Util.serialize(mg.getGraphDesc().dumpAsXml(), System.out, null, null);

		hl.addHost(mg.getProbe().getHost());
		hl.addProbe(mg.getProbe());
		
		return sp;
	}

	@Test
	public void testLoad() throws Exception {
	    Document d = Tools.parseString(goodSumSXml);
		Tools.JrdsElement je = new Tools.JrdsElement(d);
		je.addElement("element", "name=DummyHost/DummyProbe");

		HostsList hl = new HostsList();		
		Sum s = doSum(d, hl);
		Document sumDocument = s.getGraphDesc().dumpAsXml();
		jrds.Util.serialize(sumDocument, System.out, null, null);
		logger.trace(sumDocument);
	}

	@Test
	public void testRoles() throws Exception {
		Document d = Tools.parseString(goodSumSXml);
		Tools.JrdsElement je = new Tools.JrdsElement(d);
		je.addElement("element", "name=DummyHost/DummyProbe");
		je.addElement("role").setTextContent("role1");

		HostsList hl = new HostsList();
		Sum s = doSum(d, hl);

		ACL acl = s.getACL();
		Assert.assertEquals("Not an role ACL", RolesACL.class, acl.getClass());

	}

}
