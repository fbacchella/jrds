package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Tools;
import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpTest {
	static final private Logger logger = Logger.getLogger(HttpTest.class);
	
	static final private RdsHost webserver = new RdsHost();
	static final private String HOST = "testhost";

	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.setLevel(new String[] {"jrds.Util"}, logger.getLevel());
		webserver.setName(HOST);
	}

	@Test
	public void build1() {
		HttpProbe p = new HttpProbe() {
			@Override
			protected Map<String, Number> parseStream(InputStream stream) {
				return null;
			}
		};
		ProbeDesc pd = new ProbeDesc();
		p.setHost(webserver);
		p.setPd(pd);
		p.setFile("/");
		p.setPort(80);
		p.configure();
		Assert.assertEquals("http://" + HOST + ":80/", p.getUrlAsString());
	}

	@Test
	public void build2() {
		HttpProbe p = new HttpProbe() {
			@Override
			protected Map<String, Number> parseStream(InputStream stream) {
				return null;
			}
		};
		p.setHost(webserver);
		ProbeDesc pd = new ProbeDesc();
		p.setPd(pd);		
        p.setFile("/file");
        p.setPort(80);
		p.configure("/file");
		Assert.assertEquals("http://" + HOST + ":80/file", p.getUrlAsString());
	}

	@Test
	public void build3() {
		HttpProbe p = new HttpProbe() {
			@Override
			protected Map<String, Number> parseStream(InputStream stream) {
				return null;
			}
		};
		p.setHost(webserver);
		ProbeDesc pd = new ProbeDesc();
		p.setPd(pd);		
        p.setFile("/file");
        p.setPort(81);
        p.configure();
		Assert.assertEquals("http://" + HOST + ":81/file", p.getUrlAsString());
	}
	
}
