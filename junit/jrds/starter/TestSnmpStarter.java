package jrds.starter;

import jrds.Tools;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSnmpStarter {
	static Logger logger = Logger.getLogger(TestSnmpStarter.class);

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"org.snmp4j", "jrds.snmp", "jrds.starter"}, logger.getLevel());
	}

	@Test
	public void test() {
		StarterNode n1 = new StarterNode() { };
		n1.registerStarter(jrds.snmp.SnmpStarter.full);
		
		StarterNode n2 = new StarterNode(n1) { };
		SnmpStarter snmp = new SnmpStarter();
		snmp.setHostname("localhost");
		n2.registerStarter(snmp);
		
		logger.debug("Starting at level 1");
		n1.startCollect();
		logger.debug("Starting at level 2");
		n2.startCollect();

		Assert.assertTrue(n2.isCollectRunning());

		logger.debug("Stopping at level 1");
		n1.stopCollect();

		Assert.assertFalse(n2.isCollectRunning());

	}
}
