package jrds.snmp;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.HostsList;
import jrds.Log4JRule;
import jrds.Tools;
import jrds.mockobjects.SnmpAgent;
import jrds.probe.snmp.RdsSnmpSimple;
import jrds.starter.HostStarter;

public class TestSnmpStarter {
    static private SnmpAgent agent;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        agent = new SnmpAgent();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "org.snmp4j.agent.request.SnmpRequest", "org.snmp4j", "jrds.snmp", "jrds.Starter.SnmpConnection", "jrds.starter", "jrds.mockobjects");
    }

    private HostsList registerHost(HostStarter h, RdsSnmpSimple probe) throws IOException {
        HostsList hl = new HostsList(Tools.makePm(testFolder)) {
            @Override
            public boolean isCollectRunning() {
                return true;
            }

            @Override
            public int getTimeout() {
                return 1;
            }

            @Override
            public int getStep() {
                return 300;
            }
        };
        hl.addHost(h.getHost());
        h.setParent(hl);

        h.registerStarter(new MainStarter());

        SnmpConnection snmp = new SnmpConnection();
        snmp.setPort(agent.getPort());
        h.registerStarter(snmp);
        logger.debug("SNMP starter:" + snmp);

        probe.setHost(h);
        probe.configure();
        h.addProbe(probe);

        return hl;
    }

    @Test
    public void testSucess() throws IOException {
        HostStarter n1 = new HostStarter(new HostInfo("127.0.0.1"));
        n1.setTimeout(2);
        RdsSnmpSimple n2 = new RdsSnmpSimple();
        n2.setTimeout(2);
        HostsList hl = registerHost(n1, n2);

        agent.run();
        logger.debug("Starting at level 1");
        hl.startCollect();

        logger.debug("Starting at level 2");
        n1.startCollect();
        // True because some starters are allowed to fail
        Assert.assertTrue(n1.isCollectRunning());

        logger.debug("Starting at level 3");
        n2.startCollect();

        n2.isCollectRunning();
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
    public void testFail() throws IOException {
        HostInfo h = new HostInfo("127.0.0.1");
        HostStarter n1 = new HostStarter(h);
        RdsSnmpSimple n2 = new RdsSnmpSimple();
        HostsList hl = registerHost(n1, n2);

        logger.debug("Starting at level 1");
        hl.startCollect();

        logger.debug("Starting at level 2");
        n1.startCollect();
        // True because some starters are allowed to fail
        Assert.assertTrue(n1.isCollectRunning());

        logger.debug("Starting at level 3");
        n2.startCollect();

        logger.debug("{}", n2.find(SnmpConnection.class));
        Assert.assertEquals("Uptime not 0", 0, n2.find(SnmpConnection.class).getUptime());

        logger.debug("Stopping at level 2");
        n1.stopCollect();

        Assert.assertFalse(n2.isCollectRunning());
        Assert.assertFalse(n1.isCollectRunning());
    }

}
