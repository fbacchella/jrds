package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.probe.JMXConnection;

public class TestConnectionInfo {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.starter", "jrds.Starter", "jrds.probe.JMXConnection");
    }

    @Test
    public void testRegister() throws InvocationTargetException, NoSuchMethodException, SecurityException {
        ConnectionInfo ci = new ConnectionInfo(JMXConnection.class, "jrds.probe.JMXConnection", Collections.emptyList(), Collections.singletonMap("user", "admin"));
        StarterNode sn = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        ci.register(sn);
    }

}
