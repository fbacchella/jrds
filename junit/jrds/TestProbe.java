package jrds;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import jrds.mockobjects.MokeProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.DsType;
import org.rrd4j.core.Sample;

public class TestProbe {
	static final private Logger logger = Logger.getLogger(TestProbe.class);
	static private final Random r= new Random();
	static private final String[] dirs = new String[] {"configdir", "rrddir", "tmpdir"};

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.Probe", "jrds.ProbeDesc", "jrds.Probe"}, logger.getLevel());
		PropertiesManager pm = new PropertiesManager();
		pm.setProperty("configdir", "tmp");
		pm.setProperty("rrddir", "tmp");
		pm.update();
		StoreOpener.prepare(pm.dbPoolSize, pm.syncPeriod, pm.timeout, null);
	}
	
	@Test
	public void testHighLow() throws TransformerException, IOException, ParserConfigurationException {
		ProbeDesc pd = new ProbeDesc();
		pd.setName("empty");
		pd.setProbeName("empty");
		Map<String, Object> dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "ds0");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collecthigh", "high");
		dsMap.put("collectlow", "low");
		pd.add(dsMap);
		System.out.println();
		
		RdsHost host = new RdsHost() {
			@Override
			public boolean isCollectRunning() {
				return true;
			}
		};
		MokeProbe<String, Long> p = new MokeProbe<String, Long>(pd) {
			@Override
			public boolean isCollectRunning() {
				return true;
			}			
			@Override
			public void modifySample(Sample oneSample, Map<String, Long> values) {
				oneSample.setTime(getLastUpdate().getTime() + 1000);
				super.modifySample(oneSample, values);
			}			
		};
		host.setName("DummyHost");
		host.setHostDir(new File("tmp"));
		p.setHost(host);
		p.configure();
		System.out.println();
		Map<String, Long> val = new HashMap<String, Long>();
		long high =  255L;
		long low = 64L;
		val.put("high", high);
		val.put("low", low);
		p.checkStore();
		p.injectValues(val);
		p.collect();
		Assert.assertEquals("32 + 32 to 64 failed", (high << 32) + low, p.getLastValues().get("ds0").doubleValue(), 0.1);
	}
	
	@Test
	public void testDefault() throws TransformerException, IOException, ParserConfigurationException {
		ProbeDesc pd = new ProbeDesc();
		pd.setName("empty");
		pd.setProbeName("empty");
		Map<String, Object> dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "ds0");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("defaultValue", "1");
		pd.add(dsMap);
		dsMap.clear();
		dsMap.put("dsName", "ds1");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("defaultValue", "1");
		pd.add(dsMap);
		System.out.println();
		
		RdsHost host = new RdsHost() {
			@Override
			public boolean isCollectRunning() {
				return true;
			}
		};
		MokeProbe<String, Long> p = new MokeProbe<String, Long>(pd) {
			@Override
			public boolean isCollectRunning() {
				return true;
			}
			@Override
			public void modifySample(Sample oneSample, Map<String, Long> values) {
				oneSample.setTime(getLastUpdate().getTime() + 1000);
				super.modifySample(oneSample, values);
			}			
		};
		host.setName("DummyHost");
		host.setHostDir(new File("tmp"));
		p.setHost(host);
		p.configure();
		System.out.println();
		Map<String, Long> val = new HashMap<String, Long>();
		val.put("ds1", 2L);

		p.checkStore();
		p.injectValues(val);
		p.collect();
		Assert.assertEquals("Default value is not inserted", 1, p.getLastValues().get("ds0").doubleValue(), 0.1);
		Assert.assertEquals("Default value overwrite read value", 2, p.getLastValues().get("ds1").doubleValue(), 0.1);
	}
}
