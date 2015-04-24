package jrds.starter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

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
        public void listen() throws Exception {
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

    static Logger logger = Logger.getLogger(ListenerTest.class);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();

        logger.setLevel(Level.TRACE);
        Tools.setLevel(new String[] {StarterNode.class.toString(), Starter.class.toString()}, logger.getLevel());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void test1() {
        PassiveProbe pp = new PassiveProbe();
        pp.setListener(new PassiveListener());
    }
}
