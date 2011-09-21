package jrds;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import jrds.graphe.Sum;
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
        HostsList hl = new HostsList();
        hl.configure(new PropertiesManager());

        ArrayList<String> graphlist = new ArrayList<String>();
		graphlist.add("badhost/badgraph");
		Sum s = new Sum("emptysum", graphlist);
		s.configure(hl);
		Graph g = s.getGraph();
		g.setPeriod(new Period());
		RrdGraphDef rgd = g.getRrdGraphDef();
		Assert.assertNotNull(rgd);
		RrdGraph graph = new RrdGraph(rgd);
		logger.debug(graph.getRrdGraphInfo().getHeight());
		logger.debug(graph.getRrdGraphInfo().getWidth());
		logger.debug(rgd.toString());
	}

}
