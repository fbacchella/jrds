package jrds;

import java.io.File;
import java.io.IOException;

import jrds.mockobjects.Full;
import jrds.thresholds.Threshold;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.core.RrdDb;

public class TestThreshold {
	static final private Logger logger = Logger.getLogger(TestThreshold.class);

	static Probe p = null;
	static RrdDb db = null;

	@BeforeClass
	static public void configure() throws IOException  {
		Tools.configure();
		logger.setLevel(Level.ERROR);
		Tools.setLevel(new String[] {"jrds.Probe", Threshold.class.getName()}, logger.getLevel());

		File rrdFile = new File("tmp/fullmock.rrd");
		if(rrdFile.exists())
			rrdFile.delete();

		p = Full.create();
		Full.fill(p);

		db = new RrdDb("tmp/fullmock.rrd");

	}

	@Test
	public void constructAndCheckImmediateHigh() {
		Threshold t = new Threshold("th1", "sun", 100, 0, Threshold.Comparator.HIGH);
		Assert.assertTrue(t.check(db));
	}
	@Test
	public void constructAndCheckImmediateLow() {
		Threshold t = new Threshold("th1", "sun", 100, 0, Threshold.Comparator.LOW);
		Assert.assertTrue(! t.check(db));
	}

}
