package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestConnectionInfo {
    static private final Logger logger = Logger.getLogger(TestConnectionInfo.class);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();

        logger.setLevel(Level.TRACE);
        Tools.setLevel(logger, Level.TRACE, "jrds.Starter", "jrds.starter.ConnectionInfo");
    }

    @Test
    public void testRegister() throws InvocationTargetException {
        Map<String, String> empty = Collections.emptyMap();
        ConnectionInfo ci = new ConnectionInfo(jrds.snmp.SnmpConnection.class, "jrds.snmp.SnmpConnection", Collections.emptyList(), empty);
        StarterNode sn = new StarterNode() {};
        ci.register(sn);
    }
}
