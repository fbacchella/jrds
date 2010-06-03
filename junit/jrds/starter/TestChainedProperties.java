package jrds.starter;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestChainedProperties {
	static Logger logger = Logger.getLogger(TestChainedProperties.class);

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {StarterNode.class.toString(), Starter.class.toString()}, logger.getLevel());
	}
	
	@Test
	public void testtoparent() {
		StarterNode n1 = new StarterNode() { };
		ChainedProperties s1 = new ChainedProperties();
		s1.put("level", "0");
		n1.registerStarter(s1);

		StarterNode n2 = new StarterNode(n1) { };
		ChainedProperties s2 = new ChainedProperties();
		n2.registerStarter(s2);

		Assert.assertEquals(n2.find(ChainedProperties.class).get("level"),"0");
	}
	
	@Test
	public void testhide() {
		StarterNode n1 = new StarterNode() { };
		ChainedProperties s1 = new ChainedProperties();
		s1.put("level", "0");
		n1.registerStarter(s1);

		StarterNode n2 = new StarterNode(n1) { };
		ChainedProperties s2 = new ChainedProperties();
		s2.put("level", "1");
		n2.registerStarter(s2);

		Assert.assertEquals(n2.find(ChainedProperties.class).get("level"),"1");
	}

}
