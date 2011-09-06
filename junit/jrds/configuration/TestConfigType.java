package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.configuration.ConfigType;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

public class TestConfigType {
	static final private Logger logger = Logger.getLogger(TestConfigType.class);

	@BeforeClass static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.setLevel(new String[] {"jrds"}, logger.getLevel());
		Tools.prepareXml();
	}
	
	public void checkNode(Node n, ConfigType t) {
		for(ConfigType tested: ConfigType.values()) {
			logger.trace("Compare " + t  + " with " + tested);
			if(tested == t) {
				Assert.assertTrue(t.memberof(n));
			}
			else
				Assert.assertFalse(tested.memberof(n));
		}
	}

	@Test
	public void detectGraph() throws Exception {
		Node n = Tools.parseRessource("customgraph.xml");
		checkNode(n, ConfigType.GRAPH);
	}

	@Test
	public void detectProbeDesc() throws Exception {
		Node n = Tools.parseRessource("fulldesc.xml");
		checkNode(n, ConfigType.PROBEDESC);
	}
}
