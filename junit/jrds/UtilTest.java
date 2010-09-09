package jrds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.mockobjects.DummyProbe;
import jrds.mockobjects.MokeProbe;
import jrds.xmlResources.ResourcesLocator;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.core.DsDef;
import org.w3c.dom.Document;

public class UtilTest {
	static final private Logger logger = Logger.getLogger(UtilTest.class);
	
	@BeforeClass
	static public void configure() throws IOException, ParserConfigurationException {
		Tools.configure();
		Tools.prepareXml();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.Util"}, logger.getLevel());

	}

	@Test
	public void testSiPrefix1() {
		Assert.assertEquals(3, jrds.Util.SiPrefix.k.getExponent());
		Assert.assertEquals(0, jrds.Util.SiPrefix.FIXED.getExponent());
		Assert.assertEquals(34.0, jrds.Util.SiPrefix.FIXED.evaluate(34, true));
		Assert.assertEquals(0.001, jrds.Util.SiPrefix.m.evaluate(1, true));
		Assert.assertEquals(1024.0, jrds.Util.SiPrefix.k.evaluate(1, false));
		Assert.assertEquals(1024 * 1024.0, jrds.Util.SiPrefix.M.evaluate(1, false));
	}
	
	@Test
	public void testSiPrefix2() {
		int lastExp = 24;
		for(jrds.Util.SiPrefix p: jrds.Util.SiPrefix.values()) {
			int newExp = p.getExponent();
			Assert.assertTrue(newExp - lastExp <= -1 || newExp - lastExp >= -3);
		}
	}

	@Test
	public void testSerialization1() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document d;

		d = Tools.parseRessource("customgraph.xml");
		Util.serialize(d, out, null, null);
	}

	@Test
	public void testSerialization2() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document d;
		Map<String, String> prop;
		String outBuffer;

		d = Tools.parseRessource("goodhost1.xml");
		prop = new HashMap<String, String>();
		prop.put("omit-xml-declaration", "yes");
		Util.serialize(d, out, null, prop);
		outBuffer = out.toString();
		logger.debug(outBuffer);
		Assert.assertFalse(outBuffer.contains("<?xml"));
	}

	@Test
	public void testSerialization3() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document d;
		Map<String, String> prop;
		String outBuffer;

		d = Tools.parseRessource("customgraph.xml");
		String publicId = d.getDoctype().getPublicId();
		String systemId = d.getDoctype().getSystemId();
		prop = new HashMap<String, String>();

		Util.serialize(d, out, null, prop);
		outBuffer = out.toString();
		logger.debug("out buffer for testSerialization3: \n" + outBuffer + "\n");
		//It should have auto-detected the doc type
		Assert.assertTrue(outBuffer.contains(publicId));
		Assert.assertTrue(outBuffer.contains(systemId));
		Assert.assertTrue(outBuffer.contains(d.getInputEncoding()));
	}

	@Test
	public void testSerialization4() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DummyProbe p = new DummyProbe();
		p.configure();
		Document d = p.dumpAsXml();
		URL xsl = ResourcesLocator.getResourceUrl("/jrds/xmlResources/probe.xsl");
		Assert.assertNotNull("xls transformation not found", xsl);
		out.reset();
		Util.serialize(d, out, xsl, null);
		String outBuffer = out.toString();
		logger.debug("testSerialization4: " + outBuffer);
		Assert.assertTrue("HTML doctype not found", outBuffer.contains("DOCTYPE html"));
		Assert.assertTrue("probe id not found in HTML", outBuffer.contains("pid=" + p.hashCode()));
		for(DsDef ds: p.getDsDefs()) {
			Assert.assertTrue(outBuffer.contains("dsName=" + ds.getDsName()));
		}
	}
	
	@Test
	public void testParseStringNumber1() {
		double n = Util.parseStringNumber("1", Double.class, Double.NaN);
		Assert.assertEquals(1.0, n, 0.001);
	}
	
	@Test
	public void testParseStringNumber2() {
		double n = Util.parseStringNumber(null, Double.class, Double.NaN);
		Assert.assertTrue(Double.isNaN(n));
	}
	
	@Test
	public void testParseStringNumber3() {
		double n = Util.parseStringNumber("a", Double.class, Double.NaN);
		Assert.assertTrue(Double.isNaN(n));
	}
	
	@Test
	public void testParseStringNumber4() {
		Number n = Util.parseStringNumber("1", Integer.class, 1);
		Assert.assertEquals(1, n);
	}

	@Test
	public void testParseStringNumber5() {
		int n = Util.parseStringNumber("1", 1);
		Assert.assertEquals(1, n);
	}

	@Test
	public void testParseStringNumber6() {
		double n = Util.parseStringNumber("1", 1.0d);
		Assert.assertEquals(1.0,n , 0.001);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateVariable1() {
		System.setProperty("jrds.unittest", "true");
		String evaluated = Util.evaluateVariables("${system.jrds.unittest}", Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		Assert.assertEquals("true", evaluated);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void evaluateVariable2() {
		System.setProperty("jrds.unittest", "true");
		String evaluated = Util.evaluateVariables("${novar}", Collections.EMPTY_MAP, Collections.EMPTY_MAP);
		Assert.assertEquals("${novar}", evaluated);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void evaluateVariable3() {
		Map<String, Object> var = new HashMap<String, Object>();
		
		var.put("a", "v1");
		var.put("b", 1);

		String evaluated = Util.evaluateVariables("${a} ${b}", var, Collections.EMPTY_MAP);
		Assert.assertEquals("v1 1", evaluated);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateVariable4() {
		Map<String, Object> var = new HashMap<String, Object>();
		
		var.put("FSID", "248ba235");

		String evaluated = Util.evaluateVariables("C:\\ Label:  Serial Number ${FSID}", var, Collections.EMPTY_MAP);
		Assert.assertEquals("C:\\ Label:  Serial Number 248ba235", evaluated);
	}
	
	@Test
	public void testParseTemplate1() {
		Probe<?,?> p = new MokeProbe<String, Number>();
		p.setHost(new RdsHost("Moke"));
		p.setLabel("label");
		String parsed = Util.parseTemplate("${host} ${probename} ${label}", p);
		Assert.assertEquals("Moke DummyProbe label", parsed);
	}

	@Test
	public void testParseTemplate2() {
		System.setProperty("jrds.unittest", "true");

		String parsed = Util.parseTemplate("${jrds.unittest}", System.getProperties());
		Assert.assertEquals("true", parsed);
	}
	
	@Test
	public void testParseTemplate3() {
		List<String> args = Arrays.asList("unittest");

		String parsed = Util.parseTemplate("${1}", args);
		Assert.assertEquals("unittest", parsed);
	}

	@Test
	public void testNormalization1() {
		Probe<?,?> p = new jrds.mockobjects.DummyProbe() {
			@Override
			public Date getLastUpdate() {
				return new Date();
			}
		};
		Date now = new Date();
		
		Date n = Util.endDate(p, now);
		Assert.assertTrue(Math.abs(now.getTime() - n.getTime()) < 500 * 1000);
	}

	@Test
	public void testNormalization2() {		
		Probe<?,?> p = new jrds.mockobjects.DummyProbe() {
			@Override
			public Date getLastUpdate() {
				Date now = new Date();
				Calendar calBegin = Calendar.getInstance();
				calBegin.setTime(now);
				calBegin.add(Calendar.MONTH, -4);
				return calBegin.getTime();
			}
		};

		Date now = new Date();

		Date n = Util.endDate(p, now);
		Assert.assertTrue(Math.abs(now.getTime() - n.getTime()) < 500 * 1000);
	}

	@Test
	public void testNormalization3() {		
		Probe<?,?> p = new jrds.mockobjects.DummyProbe() {
			@Override
			public Date getLastUpdate() {
				Date now = new Date();
				Calendar calBegin = Calendar.getInstance();
				calBegin.setTime(now);
				calBegin.add(Calendar.MONTH, 4);
				return calBegin.getTime();
			}
		};

		Date now = new Date();

		Date n = Util.endDate(p, now);
		Assert.assertTrue(Math.abs(now.getTime() - n.getTime()) < 500 * 1000);
	}

	@Test
	public void testSort() {
		String[] toSort =  new String[]{"zOS", "linux", "redbus", "telecity", "linode"};
		Arrays.sort(toSort, jrds.Util.nodeComparator);
		logger.trace(Arrays.asList(toSort));
		Assert.assertEquals("linode", toSort[0]);
		Assert.assertEquals("zOS", toSort[4]);
	}
}
