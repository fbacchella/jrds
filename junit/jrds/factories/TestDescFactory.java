package jrds.factories;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.ProbeDesc;
import jrds.factories.xml.JrdsNode;
import jrds.mockobjects.GetMoke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestDescFactory {
	static final private Logger logger = Logger.getLogger(TestDescFactory.class);

	static final String graphDescXml = 
		"<graphdesc>" +
		"<name>mokegraph</name>" +
		"<graphName>mokegraphname</graphName>" +
		"<graphTitle>mokegraphtitle</graphTitle>" +
		"<unit><SI/></unit>" +
		"<verticalLabel>verticallabel</verticalLabel>" +
		"<add>" + 
		"<name>addname</name>" +
		"<color>red</color>" +
		"<path><host>pathost</host><probe>pathprobe</probe><name>pathname</name></path>" + 
		"</add>" +
		"<hosttree>"  +
		"<pathelement>HOST</pathelement>" +
		"<pathelement>SERVICES</pathelement>" +
		"<pathstring>moke</pathstring>" +
		"</hosttree>" +
		"<viewtree>" +
		"<pathelement>SERVICES</pathelement>" +
		"<pathstring>moke</pathstring>" +
		"<pathelement>HOST</pathelement>" +
		"</viewtree>" +
		"</graphdesc>";

	static Loader l;

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories"}, logger.getLevel());
		Tools.setLevel(new String[] {"jrds.factories.xml.CompiledXPath"}, Level.INFO);
		Tools.prepareXml();

	}

	@Test
	public void loadGraph()  throws Exception {
		Document d = Tools.parseRessource("customgraph.xml");
		GraphDescBuilder builder = new GraphDescBuilder();
		builder.setProperty(ObjectBuilder.properties.PM, new PropertiesManager());
		GraphDesc gd = (GraphDesc) builder.build(new JrdsNode(d));

		Assert.assertEquals("name", gd.getName());
		Assert.assertEquals("graphName", gd.getGraphName());
		Assert.assertEquals("", gd.getGraphTitle());
		Assert.assertTrue(gd.isSiUnit());
		Assert.assertEquals("verticalLabel", gd.getVerticalLabel());
		GraphNode gn = new GraphNode(GetMoke.getProbe(), gd);
		logger.debug(gd.getHostTree(gn));
		logger.debug(gd.getViewTree(gn));

	}

	@Test
	public void loadGraphDesc() throws Exception {
		InputStream is = new ByteArrayInputStream(graphDescXml.getBytes());
		Document d = Tools.parseRessource(is);

		GraphDescBuilder builder = new GraphDescBuilder();
		builder.setProperty(ObjectBuilder.properties.PM, new PropertiesManager());
		GraphDesc gd = (GraphDesc) builder.build(new JrdsNode(d));

		Assert.assertEquals("mokegraph", gd.getName());
		Assert.assertEquals("mokegraphname", gd.getGraphName());
		Assert.assertEquals("mokegraphtitle", gd.getGraphTitle());
		Assert.assertTrue(gd.isSiUnit());
		Assert.assertEquals("verticallabel", gd.getVerticalLabel());
		GraphNode gn = new GraphNode(GetMoke.getProbe(), gd);
		logger.debug(gd.getHostTree(gn));
		logger.debug(gd.getViewTree(gn));
	}

	@Test
	public void loadProbeDesc() throws Exception {
		Document d = Tools.parseRessource("fulldesc.xml");
		PropertiesManager pm = new PropertiesManager();

		ProbeDescBuilder builder = new ProbeDescBuilder();
		builder.setProperty(ObjectBuilder.properties.PM, pm);
		ProbeDesc pd = builder.makeProbeDesc(new JrdsNode(d));
		Assert.assertEquals("name", pd.getName());
		Assert.assertEquals("probename", pd.getProbeName());
		Assert.assertEquals(jrds.mockobjects.MokeProbe.class, pd.getProbeClass());
		Assert.assertEquals("specificvalue1", pd.getSpecific("specificname1"));
		Assert.assertEquals("specificvalue2", pd.getSpecific("specificname2"));
		Assert.assertEquals(0.5, pd.getUptimefactor(), 0);
		Assert.assertEquals("value", pd.getProperties().get("key"));
		Assert.assertEquals((long) pm.step * 2, pd.getHeartBeatDefault());
		logger.trace(pd.getCollectMapping());
		logger.trace(pd.getCollectOids());
		logger.trace(pd.getCollectStrings());
		logger.trace(pd.getDefaultArgs());
		int base = 80;
		List<Object> defaultargs = pd.getDefaultArgs();
		for(Object o: defaultargs) {
			if(o instanceof Integer)
				Assert.assertEquals(base++, ((Integer)o).intValue());
		}
		Assert.assertEquals(82, base);
		Assert.assertTrue(defaultargs.get(2) instanceof List<?>);
		//A collect string "" should not be collected
		Assert.assertEquals(3, pd.getCollectMapping().size());
		Assert.assertEquals(1, pd.getCollectOids().size());
		Assert.assertEquals(3, pd.getCollectStrings().size());
	}
	
	@Test
	public void loadBadProbeDesc() throws Exception {
		Document d = Tools.parseRessource("baddesc.xml");
		PropertiesManager pm = new PropertiesManager();
		
		List<LoggingEvent> logged = Tools.getLockChecker("jrds.factories.ObjectBuilder");

		ProbeDescBuilder builder = new ProbeDescBuilder();
		builder.setProperty(ObjectBuilder.properties.PM, pm);
		ProbeDesc pd = builder.makeProbeDesc(new JrdsNode(d));
		logger.trace("Collect mapping: " + pd.getCollectMapping());
		logger.trace("Collect oids: " + pd.getCollectOids());
		logger.trace("Collect strings: " + pd.getCollectStrings());
		Assert.assertEquals(1, pd.getCollectMapping().size());
		Assert.assertEquals(1, pd.getCollectStrings().size());
		boolean found = false;
		for(LoggingEvent le: logged) {
			String  message = le.getRenderedMessage();
			if(message.contains("Invalid ds type specified")) {
				found = true;
				break;
			}
		}
		Assert.assertTrue("bad probe desc not detected", found);
	}

}
