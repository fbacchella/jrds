package jrds.mockobjects;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.RdsHost;

import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;

public class Full {
	static final long SEED = 1909752002L;
	static final Random RANDOM = new Random(SEED);
	static final String FILE = "fullmock";

	static final long START = Util.getTimestamp(2003, 4, 1);
	static final long END = Util.getTimestamp(2003, 5, 1);
	static final int STEP = 300;

	static final int IMG_WIDTH = 500;
	static final int IMG_HEIGHT = 300;
	
	static public ProbeDesc getPd() {
		ProbeDesc pd = new ProbeDesc();
		
		Map<String, Object> dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "sun");
		dsMap.put("dsType", DsType.GAUGE);
		pd.add(dsMap);
		
		dsMap.clear();
		dsMap.put("dsName", "shade");
		dsMap.put("dsType", DsType.GAUGE);
		pd.add(dsMap);

		pd.setName(FILE);
		pd.setProbeName(FILE);

		return pd;
	}
	
	static public GraphDesc getGd() {
		GraphDesc gd = new GraphDesc();

		gd.add("sun", null, GraphDesc.LINE.toString(), "green", null, null, null, null, null, null, null);
		gd.add("shade", null, GraphDesc.LINE.toString(), "blue", null, null, null, null, null, null, null);
		gd.add("median", "sun,shade,+,2,/", GraphDesc.LINE.toString(), "magenta", null, null, null, null, null, null, null);
		gd.add("diff", "sun,shade,-,ABS,-1,*", GraphDesc.AREA.toString(), "yellow", null, null, null, null, null, null, null);

		gd.setGraphTitle("Temperatures in May 2003");
		gd.setVerticalLabel("temperature");
		return gd;
	}
	
	static public Probe<?,?> getProbe() {
		Probe<?,?> p = new Probe<String, Number>() {

			@Override
			public Map<String, Number> getNewSampleValues() {
				return Collections.emptyMap();
			}

			@Override
			public String getSourceType() {
				return "fullmoke";
			}
		};
		p.setPd(getPd());
		return p;

	}
	static public Probe<?,?> create() {
		PropertiesManager pm = new PropertiesManager();
		pm.setProperty("rrddir", "tmp");

		RdsHost host = new RdsHost("Empty");
		host.setHostDir(new File("tmp"));
		
		Probe<?,?> p = getProbe();
		p.setHost(host);
		
		p.checkStore();
		
		return p;
	}

	static public long fill(Probe<?,?> p) throws IOException {
		long start = System.currentTimeMillis() / 1000;
		long end = start + 3600 * 24 * 30;

		String rrdPath = p.getRrdName();
		// update database
		GaugeSource sunSource = new GaugeSource(1200, 20);
		GaugeSource shadeSource = new GaugeSource(300, 10);
		long t = start;
		RrdDb rrdDb = new RrdDb(rrdPath);
		Sample sample = rrdDb.createSample();

		while (t <= end + 86400L) {
			sample.setTime(t);
			sample.setValue("sun", sunSource.getValue());
			sample.setValue("shade", shadeSource.getValue());
			sample.update();
			t += RANDOM.nextDouble() * STEP + 1;
		}

		rrdDb.close();
		
		return t;
	}
	
	static public void createRrd() throws IOException {
		println("== Starting demo");
		long start = START;
		long end = END;
		String rrdPath = Util.getRrd4jDemoPath("tmp/" + FILE + ".rrd");
		PrintWriter log = new PrintWriter(System.out);
		// creation
		println("== Creating RRD file " + rrdPath);
		RrdDef rrdDef = new RrdDef(rrdPath, start - 1, STEP);
		rrdDef.addDatasource("sun", DsType.GAUGE, STEP * 2, 0, Double.NaN);
		rrdDef.addDatasource("shade", DsType.GAUGE, STEP * 2, 0, Double.NaN);
		rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
		rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
		rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
		rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
		rrdDef.addArchive(MAX, 0.5, 1, 600);
		rrdDef.addArchive(MAX, 0.5, 6, 700);
		rrdDef.addArchive(MAX, 0.5, 24, 775);
		rrdDef.addArchive(MAX, 0.5, 288, 797);
		println(rrdDef.dump());
		log.println(rrdDef.dump());
		println("Estimated file size: " + rrdDef.getEstimatedSize());
		RrdDb rrdDb = new RrdDb(rrdDef);
		println("== RRD file created.");
		if (rrdDb.getRrdDef().equals(rrdDef)) {
			println("Checking RRD file structure... OK");
		}
		else {
			println("Invalid RRD file created. This is a serious bug, bailing out");
			return;
		}
		rrdDb.close();
		println("== RRD file closed.");

		// update database
		GaugeSource sunSource = new GaugeSource(1200, 20);
		GaugeSource shadeSource = new GaugeSource(300, 10);
		println("== Simulating one month of RRD file updates with step not larger than " +
				STEP + " seconds (* denotes 1000 updates)");
		long t = start;
		int n = 0;
		rrdDb = new RrdDb(rrdPath);
		Sample sample = rrdDb.createSample();

		while (t <= end + 86400L) {
			sample.setTime(t);
			sample.setValue("sun", sunSource.getValue());
			sample.setValue("shade", shadeSource.getValue());
			sample.update();
			t += RANDOM.nextDouble() * STEP + 1;
			if (((++n) % 1000) == 0) {
				System.out.print("*");
			}
		}

		rrdDb.close();

	}
	static void println(String msg) {
		//System.out.println(msg + " " + Util.getLapTime());
		System.out.println(msg);
	}

	static void print(String msg) {
		System.out.print(msg);
	}

	static class GaugeSource {
		private double value;
		private double step;

		GaugeSource(double value, double step) {
			this.value = value;
			this.step = step;
		}

		long getValue() {
			double oldValue = value;
			double increment = RANDOM.nextDouble() * step;
			if (RANDOM.nextDouble() > 0.5) {
				increment *= -1;
			}
			value += increment;
			if (value <= 0) {
				value = 0;
			}
			return Math.round(oldValue);
		}
	}

}
