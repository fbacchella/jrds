package jrds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import jrds.mockobjects.GetMoke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GraphTest {
	static final Logger logger = Logger.getLogger(GraphTest.class);
	static HostsList hl;
	@Test 
	public void getBytes() throws IOException {
		Probe p = GetMoke.getProbe();
		GraphNode gn = new GraphNode(p, GetMoke.getGraphDesc());
		Period pr = new Period();
		Graph g = new Graph(gn);
		g.setPeriod(pr);
		File outputFile =  new File("tmp/mock.png");
		OutputStream out = new FileOutputStream(outputFile);
		g.writePng(out);
		Assert.assertTrue(outputFile.isFile());
		Assert.assertTrue(outputFile.length() > 0);
	}
	@Test public void compare() throws IOException {
		Probe p = GetMoke.getProbe();
		GraphNode gn = new GraphNode(p, GetMoke.getGraphDesc());
		Period pr = new Period();
		Graph g1 = new Graph(gn);
		g1.setPeriod(pr);
		Graph g2 = new Graph(gn);
		g2.setPeriod(pr);
		Assert.assertEquals(g1.hashCode(), g2.hashCode());
		Assert.assertEquals(g1, g2);
	}

	@BeforeClass static public void configure() throws IOException {
		Tools.configure();
		logger.setLevel(Level.ERROR);
		Tools.setLevel(new String[] {"jrds.Graph"}, logger.getLevel());
		PropertiesManager pm = new PropertiesManager();
		//Not sure to find the descriptions in test environnement
		if(PropertiesManager.class.getResource("/desc") == null)
			pm.libspath.add(new URL("file:desc"));
		hl = new HostsList(pm);
	}

}
