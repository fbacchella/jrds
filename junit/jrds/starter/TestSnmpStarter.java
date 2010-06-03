package jrds.starter;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSnmpStarter {
	static Logger logger = Logger.getLogger(StarterTest.class);

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {jrds.snmp.SnmpStarter.class.toString(), StarterNode.class.toString(), Starter.class.toString()}, logger.getLevel());
	}

	@Test
	public void test() {
		StarterNode n1 = new StarterNode() { };
		n1.registerStarter(jrds.snmp.SnmpStarter.full);
		
		n1.startCollect();

		Assert.assertTrue(n1.isCollectRunning());

	}
}
