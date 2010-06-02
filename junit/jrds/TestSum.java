package jrds;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import jrds.probe.SumProbe;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class TestSum {
	static final private Logger logger = Logger.getLogger(TestSum.class);

	@BeforeClass
	static public void configure() throws IOException, ParserConfigurationException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {}, logger.getLevel());
		Tools.prepareXml();
	}
	
	@Test
	public void emptysum() throws Exception {
		ArrayList<String> graphlist = new ArrayList<String>();
		graphlist.add("badhost/badgraph");
		SumProbe s = new SumProbe("emptysum", graphlist);
		RdsHost sumhost = new RdsHost();
		HostsList hl = new HostsList();
		hl.configure(new PropertiesManager());
		hl.addHost(sumhost);
		s.setHost(sumhost);
		GraphNode gn = s.getGraphList().iterator().next();
		Graph g = gn.getGraph();
		g.setPeriod(new Period());
		RrdGraphDef rgd = g.getRrdGraphDef();
		Assert.assertNotNull(rgd);
		RrdGraph graph = new RrdGraph(rgd);
		logger.debug(graph.getRrdGraphInfo().getHeight());
		logger.debug(graph.getRrdGraphInfo().getWidth());
		logger.debug(rgd.toString());
	}

}
