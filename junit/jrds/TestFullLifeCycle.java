package jrds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import jrds.mockobjects.Full;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFullLifeCycle {
	static final private Logger logger = Logger.getLogger(TestFullLifeCycle.class);

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.Graph", "jrds.GraphNode"}, logger.getLevel());
	}

	@Test
	public void create() throws IOException {
		PropertiesManager pm = new PropertiesManager();
		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.update();
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, pm.rrdbackend);

		File rrdFile = new File("tmp", "fullmock.rrd");
		if(rrdFile.exists())
			rrdFile.delete();

		Probe p = Full.create();
		long endSec = Full.fill(p);
		
		logger.debug(p.getLastUpdate());
		
		Date end = org.rrd4j.core.Util.getDate(endSec);
		Calendar calBegin = Calendar.getInstance();
		calBegin.setTime(end);
		calBegin.add(Calendar.MONTH, -1);
		Date begin = calBegin.getTime();
		
		end = jrds.Util.normalize(end, p.getStep());

		Period pr = new Period();
		pr.setEnd(end);
		pr.setBegin(begin);

		GraphNode gn = new GraphNode(p, Full.getGd());
		Graph g = new Graph(gn);
		g.setPeriod(pr);

		File outputFile =  new File("tmp", "fullmock.png");
		OutputStream out = new FileOutputStream(outputFile);
		g.writePng(out);

		StoreOpener.stop();
		
		Assert.assertTrue(rrdFile.exists());
		Assert.assertTrue(rrdFile.length() > 0);

	}
}
