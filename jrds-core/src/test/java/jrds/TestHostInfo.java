package jrds;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.probe.JMXConnection;
import jrds.starter.ConnectionInfo;
import jrds.starter.HostStarter;

public class TestHostInfo {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.starter.ConnectionInfo", "jrds.snmp.SnmpConnection", "jrds.HostInfo");
    }

    @Test
    public void instantiate() throws InvocationTargetException {
        ConnectionInfo ci = new ConnectionInfo(JMXConnection.class, "jrds.probe.JMXConnection", Collections.emptyList(), Collections.singletonMap("user", "admin"));

        HostInfo hi = new HostInfo("localhost");
        hi.addConnection(ci);
        HostStarter hs = new HostStarter(hi);
        ci.register(hs);
        Assert.assertEquals("connection not found", "jrds.probe.JMXConnection@localhost", hs.find(JMXConnection.class).toString());
    }

}
