package jrds.starter;

import jrds.RdsHost;
import jrds.Tools;
import jrds.mockobjects.SnmpAgent;
import jrds.probe.snmp.RdsSnmpSimple;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSnmpStarter {
	static private final Logger logger = Logger.getLogger(TestSnmpStarter.class);
	static private SnmpAgent agent;

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"org.snmp4j", "jrds.snmp", "jrds.Starter", "jrds.starter", "jrds.mockobjects"}, logger.getLevel());
		agent = new SnmpAgent();
	}

	@Test
	public void testSucess() {
		agent.run();
		RdsHost n1 = new RdsHost("127.0.0.1") { };
		n1.registerStarter(jrds.snmp.SnmpStarter.full);
		SnmpStarter snmp = new SnmpStarter();
		snmp.setHostname(n1.getDnsName());
		snmp.setPort(agent.getPort());
		logger.debug("SNMP starter:" + snmp);
		n1.registerStarter(snmp);

		RdsSnmpSimple n2 = new RdsSnmpSimple() { };
		n2.setHost(n1);
		n2.configure();

		logger.debug("Starting at level 1");
		n1.startCollect();
		//True because some starters are allowed to fail
		Assert.assertTrue(n1.isCollectRunning());

		logger.debug("Starting at level 2");
		n2.startCollect();

		Assert.assertTrue(n2.isCollectRunning());

		logger.debug("Stopping at level 1");
		n1.stopCollect();

		Assert.assertFalse(n2.isCollectRunning());
		Assert.assertFalse(n1.isCollectRunning());
		agent.stop();

	}
	
	@Test
	public void testFail() {
		RdsHost n1 = new RdsHost("127.0.0.1") { };
		n1.registerStarter(jrds.snmp.SnmpStarter.full);
		SnmpStarter snmp = new SnmpStarter();
		snmp.setHostname(n1.getDnsName());
		snmp.setPort(agent.getPort());
		logger.debug("SNMP starter:" + snmp);
		n1.registerStarter(snmp);

		RdsSnmpSimple n2 = new RdsSnmpSimple() { };
		n2.setHost(n1);
		n2.configure();

		logger.debug("Starting at level 1");
		n1.startCollect();
		//True because some starters are allowed to fail
		Assert.assertTrue(n1.isCollectRunning());

		logger.debug("Starting at level 2");
		n2.startCollect();

		Assert.assertFalse(n2.isCollectRunning());

		logger.debug("Stopping at level 1");
		n1.stopCollect();

		Assert.assertFalse(n2.isCollectRunning());
		Assert.assertFalse(n1.isCollectRunning());

	}
}
