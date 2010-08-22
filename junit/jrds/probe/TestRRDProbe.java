package jrds.probe;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRRDProbe {
	static final private Logger logger = Logger.getLogger(TestRRDProbe.class);

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] { "jrds.Probe" }, logger.getLevel());

		Process p = Runtime.getRuntime().exec(new String[] {"rrdtool", "restore", "-", "tmp/rrdtool.rrd"});
//		InputStreamReader stdout = new InputStreamReader(p.getInputStream());
//		InputStreamReader stderr = new InputStreamReader(p.getErrorStream());
//		InputStreamReader[] out = new InputStreamReader[] {stdout, stderr};
		OutputStreamWriter stdin = new OutputStreamWriter(p.getOutputStream());

//		for(InputStreamReader std: out) {
//			BufferedReader r = new BufferedReader(std);
//			String line = r.readLine();
//			while (line != null) {
//				System.out.print(line);
//				line = r.readLine();
//			}
//		}
		InputStreamReader rrdxml = new InputStreamReader(Tools.class.getResourceAsStream("/ressources/rrdtool.xml"));
		BufferedReader r = new BufferedReader(rrdxml);
		String line = r.readLine();
		while (line != null) {
			stdin.append(line);
			line = r.readLine();
		}
		r.close();
		stdin.flush();
		stdin.close();
		logger.trace("wait for: " + p.waitFor());
	}
	
	@Test
	public void test1() throws IOException {
		RRDToolProbe p = new RRDToolProbe();
		p.setHost(new RdsHost("toto"));
		ProbeDesc pd = new ProbeDesc();
		pd.setName("Rrdtool");	
		pd.setProbeName("rrdtool");
		p.setPd(pd);
		
		logger.trace("rrd file state " + new File("tmp/rrdtool.rrd").canRead());
		Assert.assertTrue("Configuration of tmp/rrdtool.rrd failed", p.configure(new File("tmp/rrdtool.rrd")));
		Assert.assertTrue("Configuration of tmp/rrdtool.rrd failed", p.checkStore());
		GraphDesc gd = new GraphDesc();
		gd.setGraphName("rrdtool");
		gd.setName("rrdtool");
		gd.add("speed", "speed", null, GraphDesc.LINE, Color.BLUE, "speed", GraphDesc.DEFAULTCF, false, null, null);
		gd.add("weight", "weight", null, GraphDesc.LINE, Color.GREEN, "weight", GraphDesc.DEFAULTCF, false, null, null);
		p.addGraph(gd);
		for(GraphNode gn: p.getGraphList()) {
			Graph g = gn.getGraph();
			long start = 920802300;
			long end = 920808900;
			long step = 300;

			g.setEnd(new Date(end * 1000));
			g.setStart(new Date(start * 1000));
			File outputFile =  new File("tmp", "rrdtool.png");
			OutputStream out = new FileOutputStream(outputFile);
			g.writePng(out);
			
			logger.trace(gn.getClass());
			logger.trace(gn.getCustomData());
			jrds.ProxyPlottableMap pmap = gn.getCustomData();
			pmap.configure(start, end, step);
			for(long i = start; i < end; i += step) {
				logger.trace(new Date(i * 1000) + ":" + pmap.get("speed").getValue(i) + ":" + pmap.get("weight").getValue(i));
				
			}
		}
		
	}

}
