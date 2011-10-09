package jrds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jrds.mockobjects.MockGraph;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGraphTree {
	static final private Logger logger = Logger.getLogger(TestGraphTree.class);
	
	@BeforeClass
	static public void configure() throws IOException, ParserConfigurationException {
		Tools.configure();
		Tools.prepareXml();
		Tools.setLevel(logger, Level.TRACE, "jrds.GraphTree");
	}
	
	private List<String> doList(String... pathelems) {
		return new ArrayList<String>( Arrays.asList(pathelems));
	}
	
	@Test
	public void test1() {
		GraphTree gt1 = GraphTree.makeGraph("root");
		
		GraphNode gn = new MockGraph();
		gt1.addGraphByPath(doList("a", "b", gn.getName()), gn);
		
		Assert.assertNotNull("Graph node not found" , gt1.getByPath("root", "a", "b"));
	}

	@Test
	public void test2() {
		GraphTree gt1 = GraphTree.makeGraph("root");
		
		gt1.addPath("a", "b");
		
		Assert.assertNotNull("Graph node not found" , gt1.getByPath("root", "a", "b"));
	}

}
