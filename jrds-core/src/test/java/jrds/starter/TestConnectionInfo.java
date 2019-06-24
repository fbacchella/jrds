package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.probe.jdbc.JdbcConnection;

public class TestConnectionInfo {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.starter", "jrds.Starter", "jrds.starter.ConnectionInfo");
    }

    @Test
    public void testRegister() throws InvocationTargetException {
        ConnectionInfo ci = new ConnectionInfo(JdbcConnection.class, "jrds.probe.jdbc.JdbcConnection", Collections.emptyList(), Collections.singletonMap("user", "admin"));
        StarterNode sn = new StarterNode() {};
        ci.register(sn);
    }
}
