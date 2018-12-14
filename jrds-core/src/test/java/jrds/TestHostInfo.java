package jrds;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import jrds.probe.jdbc.JdbcConnection;
import jrds.starter.ConnectionInfo;
import jrds.starter.HostStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestHostInfo {
    static final private Logger logger = Logger.getLogger(TestHostInfo.class);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.starter.ConnectionInfo", "jrds.snmp.SnmpConnection", "jrds.HostInfo");
    }

    @Test
    public void instantiate() throws InvocationTargetException {
        Map<String, String> empty = Collections.emptyMap();
        ConnectionInfo ci = new ConnectionInfo(JdbcConnection.class, "jrds.probe.jdbc.JdbcConnection", Collections.emptyList(), empty);

        HostInfo hi = new HostInfo("localhost");
        hi.addConnection(ci);
        HostStarter hs = new HostStarter(hi);
        ci.register(hs);
        Assert.assertEquals("connection not found", "jrds.probe.jdbc.JdbcConnection@localhost", hs.find(JdbcConnection.class).toString());
    }

}
