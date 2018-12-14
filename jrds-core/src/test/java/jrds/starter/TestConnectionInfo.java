package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import jrds.Tools;
import jrds.probe.jdbc.JdbcConnection;

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
        Tools.setLevel(logger, Level.TRACE, "jrds.starter", "jrds.Starter", "jrds.starter.ConnectionInfo");
    }

    @Test
    public void testRegister() throws InvocationTargetException {
        ConnectionInfo ci = new ConnectionInfo(JdbcConnection.class, "jrds.probe.jdbc.JdbcConnection", Collections.emptyList(), Collections.singletonMap("user", "admin"));
        StarterNode sn = new StarterNode() {};
        ci.register(sn);
    }
}
