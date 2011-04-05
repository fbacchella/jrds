package jrds.factories;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.StoreOpener;
import jrds.Tools;
import jrds.factories.xml.JrdsNode;
import jrds.mockobjects.MokeProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.DsType;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.RrdGraphInfo;


public class TestGraphDescBuilder {
	static final private Logger logger = Logger.getLogger(TestGraphDescBuilder.class);

	static final ObjectBuilder ob = new ObjectBuilder() {
		@Override
		Object build(JrdsNode n) {
			return null;
		}
		
	};
	
	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.GraphDesc", "jrds.Grap"}, logger.getLevel());

		Tools.prepareXml();
		PropertiesManager pm = new PropertiesManager();
		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.update();
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, null);
	}
	
	@Test
	public void testGraphDesc() throws Exception {
		JrdsNode d = new JrdsNode(Tools.parseRessource("graphdesc.xml"));
		GraphDescBuilder gdbuild = new GraphDescBuilder();
		gdbuild.setProperty(ObjectBuilder.properties.PM, new PropertiesManager());
		GraphDesc gd = gdbuild.makeGraphDesc(d);
		MokeProbe<String, Number> p = new MokeProbe<String, Number>();

		ProbeDesc pd = p.getPd();

		Map<String, Object> dsMap = new HashMap<String, Object>(2);
		dsMap.put("dsName", "machin bidule");
		dsMap.put("dsType", DsType.COUNTER);
		pd.add(dsMap);

		dsMap.clear();
		dsMap.put("dsName", "add2");
		dsMap.put("dsType", DsType.COUNTER);
		pd.add(dsMap);
		
		p.checkStore();
				
		RrdGraphDef def = gd.getGraphDef(p);
		RrdGraphInfo gi = new RrdGraph(def).getRrdGraphInfo();
		
		logger.debug(Arrays.asList(gi.getPrintLines()));

		Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
		Assert.assertEquals("graph title failed", "graphTitle", gd.getGraphTitle());
		Assert.assertEquals("graph name failed", "name", gd.getName());
		Assert.assertEquals("legeng count failed", 2, gd.getLegendLines());
		
		Assert.assertEquals("graph height invalid", 286 , gi.getHeight());
		Assert.assertEquals("graph width invalid", 669 , gi.getWidth());
		Assert.assertEquals("graph byte count invalid", 12574 , gi.getByteCount(), 1000);
	}

	@Test
	public void testCustomGraph() throws Exception {
		JrdsNode d = new JrdsNode(Tools.parseRessource("customgraph.xml"));
		GraphDescBuilder gdbuild = new GraphDescBuilder();
		gdbuild.setProperty(ObjectBuilder.properties.PM, new PropertiesManager());
		GraphDesc gd = gdbuild.makeGraphDesc(d);
		Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
		Assert.assertEquals("graph title failed", "graphTitle", gd.getGraphTitle());
		Assert.assertEquals("graph name failed", "name", gd.getName());
	}

}
