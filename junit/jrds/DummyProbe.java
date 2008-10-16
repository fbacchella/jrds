package jrds;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.DsType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DummyProbe extends Probe {

	Class<? extends Probe> originalProbe;

	public DummyProbe() {
		this.originalProbe = null;
		ProbeDesc pd = new ProbeDesc();
		pd.setName("DummyProbe");
		pd.setProbeName("dummyprobe");
		setPd(pd);
		RdsHost host = new RdsHost();
		host.setName("DummyHost");
		setHost(host);
		Map<String, Object> dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "a");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collectKey", "/jrdsstats/stat[@key='a']/@value");
		pd.add(dsMap);
		dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "b");
		dsMap.put("dsType", DsType.COUNTER);
		dsMap.put("collectKey", "/jrdsstats/stat[@key='b']/@value");
		dsMap = new HashMap<String, Object>();
		dsMap.put("dsName", "c");
		dsMap.put("dsType", DsType.COUNTER);
		pd.add(dsMap);
	}

	public DummyProbe(Class<? extends Probe> originalProbe) {
		this.originalProbe = originalProbe;
	}

	@Override
	public Map getNewSampleValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceType() {
		// TODO Auto-generated method stub
		return null;
	}

	@BeforeClass static public void configure() {
		JrdsTester.configure();
	}

	@Test
	public void GetDetails() throws Exception, IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Document xmlDesc = dumpAsXml(true);
		Source source = new DOMSource(xmlDesc);

		TransformerFactory tFactory = TransformerFactory.newInstance();
		Source stylesource = new StreamSource(jrds.xmlResources.ResourcesLocator.getResource("probe.xsl"));
		Transformer transformer = tFactory.newTransformer(stylesource);


		StreamResult result = new StreamResult(os);
		transformer.transform(source, result);
		os.flush();
		System.out.println(os);

		DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
		instance.setIgnoringComments(false);
		instance.setValidating(true);
		instance.setCoalescing(true);
		instance.setIgnoringElementContentWhitespace(false);
		//instance.setFeature("http://xml.org/sax/features/validation", true);
		DocumentBuilder	dbuilder = instance.newDocumentBuilder();
		Document d = dbuilder.parse(new ByteArrayInputStream(os.toByteArray()));
		NodeList nl = d.getElementsByTagName("script");
		int i = 0; 
		for(Node l = nl.item(i); i < nl.getLength(); i++) {
			System.out.println(l.getTextContent().length());
		}
	}

}
