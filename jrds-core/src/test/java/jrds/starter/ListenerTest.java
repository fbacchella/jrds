package jrds.starter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.ProbeBean;
import jrds.probe.PassiveProbe;

public class ListenerTest {
    @SuppressWarnings("rawtypes")
    @ProbeBean({"dummy"})
    public static final class PassiveListener extends Listener {

        @Override
        public void register(PassiveProbe p) {
        }

        public void setDummy(String dummy) {

        }

        public String getDummy() {
            return "dummy";
        }

        @Override
        public void listen() {
        }

        @Override
        protected String identifyHost(Object message) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String identifyProbe(Object message) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String getHost(PassiveProbe pp) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getSourceType() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, StarterNode.class.toString(), Starter.class.toString());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void test1() {
        PassiveProbe pp = new PassiveProbe();
        pp.setListener(new PassiveListener());
    }
}
