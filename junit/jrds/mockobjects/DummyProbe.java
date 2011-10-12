package jrds.mockobjects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.objects.RdsHost;
import jrds.objects.probe.Probe;
import jrds.objects.probe.ProbeDesc;
import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.DsType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class DummyProbe extends Probe<String, Number> {

	Class<? extends Probe<?,?>> originalProbe;

	public void configure(Class<? extends Probe<?,?>> originalProbe) {
		this.originalProbe = originalProbe;
		configure();
	}

	public void configure() {
		ProbeDesc pd = new ProbeDesc();
		pd.setName("DummyProbe");
		pd.setProbeName("dummyprobe");
		setPd(pd);
		if(getHost() == null) {
			RdsHost host = new RdsHost();
			host.setName("DummyHost");
			host.setHostDir(new File("tmp"));
			setHost(host);
		}
		Map<String, Object> dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "ds0");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collectKey", "/jrdsstats/stat[@key='a']/@value");
		pd.add(dsMap);
		dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "ds1");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collectKey", "/jrdsstats/stat[@key='b']/@value");
		pd.add(dsMap);
		dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "ds2");
		dsMap.put("dsType", DsType.COUNTER);
		pd.add(dsMap);
	}

	@Override
	public Map<String, Number> getNewSampleValues() {
		return Collections.emptyMap();
	}

	@Override
	public String getSourceType() {
		return getClass().getName();
	}

	@BeforeClass
	static public void prepare() throws IOException, ParserConfigurationException {
		Tools.configure();
		Tools.prepareXml();
		//Logger.getLogger(EntityResolver.class).setLevel(Level.TRACE);
		//Logger.getLogger(DummyProbe.class).setLevel(Level.TRACE);
	}

	@Test(expected=NullPointerException.class)
	public void GetDetails() throws Exception, IOException
	{
		for(Class<?> c : this.getClass().getInterfaces()) {
			System.out.println(c);
		}

		configure();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Document xmlDesc = dumpAsXml(true);
		jrds.Util.serialize(xmlDesc, os, jrds.xmlResources.ResourcesLocator.getResourceUrl("probe.xsl"), null);
		Logger.getLogger(this.getClass()).trace(os.toString());
		Document d = Tools.dbuilder.parse(new ByteArrayInputStream(os.toByteArray()));
		NodeList nl = d.getElementsByTagName("a");
		for(int i=0; i < nl.getLength(); i++) {
			String expected = "ds" + i;
			Assert.assertEquals(expected, nl.item(i).getTextContent());
		}
	}

}
