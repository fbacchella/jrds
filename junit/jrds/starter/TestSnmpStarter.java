package jrds.starter;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.mockobjects.SnmpAgent;
import jrds.probe.snmp.RdsSnmpSimple;
import jrds.snmp.MainStarter;
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
		Tools.setLevel(new String[] {"org.hostslist", "org.snmp4j.agent.request.SnmpRequest", "org.snmp4j", "jrds.snmp", "jrds.Starter.SnmpStarter", "jrds.starter", "jrds.mockobjects"}, logger.getLevel());
		agent = new SnmpAgent();
	}
	
	private HostsList registerHost(RdsHost h, RdsSnmpSimple probe) {
		HostsList hl = new HostsList(new PropertiesManager()) {
			@Override
			public boolean isCollectRunning() {
				return true;
			}
		};
		hl.addHost(h);
		
		hl.registerStarter(new MainStarter());
		
		SnmpStarter snmp = new SnmpStarter();
		snmp.setHostname(h.getDnsName());
		snmp.setPort(agent.getPort());
		logger.debug("SNMP starter:" + snmp);
		h.registerStarter(snmp);

		probe.setHost(h);
		probe.configure();

		return hl;
	}

	@Test
	public void testSucess() {
		RdsHost n1 = new RdsHost("127.0.0.1") { };
		RdsSnmpSimple n2 = new RdsSnmpSimple() { };
		HostsList hl = registerHost(n1, n2);
		

		agent.run();
		logger.debug("Starting at level 1");
		hl.startCollect();

		logger.debug("Starting at level 2");
		n1.startCollect();
		//True because some starters are allowed to fail
		Assert.assertTrue(n1.isCollectRunning());

		logger.debug("Starting at level 3");
		n2.startCollect();

		Assert.assertTrue(n2.isCollectRunning());

		logger.debug("Stopping at level 2");
		n1.stopCollect();

		logger.debug("Stopping at level 1");
		hl.stopCollect();

		Assert.assertFalse(n2.isCollectRunning());
		Assert.assertFalse(n1.isCollectRunning());
		agent.stop();
	}
	
	@Test
	public void testFail() {
		RdsHost n1 = new RdsHost("127.0.0.1") { };
		RdsSnmpSimple n2 = new RdsSnmpSimple() { };
		HostsList hl = registerHost(n1, n2);

		logger.debug("Starting at level 1");
		hl.startCollect();

		logger.debug("Starting at level 2");
		n1.startCollect();
		//True because some starters are allowed to fail
		Assert.assertTrue(n1.isCollectRunning());

		logger.debug("Starting at level 3");
		n2.startCollect();

		Assert.assertFalse(n2.isCollectRunning());

		logger.debug("Stopping at level 2");
		n1.stopCollect();

		Assert.assertFalse(n2.isCollectRunning());
		Assert.assertFalse(n1.isCollectRunning());

	}

}
