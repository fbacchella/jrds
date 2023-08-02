package jrds.starter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.HostsList;
import jrds.Log4JRule;
import jrds.Tools;

public class StarterTest {
    static class LocalStarter extends Starter {
        Object key = LocalStarter.class;

        LocalStarter(Object key) {
            this.key = key;
        }

        public static Object makeKey(jrds.starter.StarterNode node) {
            return LocalStarter.class;
        }

        /*
         * (non-Javadoc)
         * 
         * @see jrds.starter.Starter#getKey()
         */
        @Override
        public Object getKey() {
            return key;
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

    @Test
    public void simpleStarter() {
        StarterNode n = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        Starter s = new Starter() {
        };
        n.registerStarter(s);

        Assert.assertNotNull(n.find(s.getClass()));
    }

    @Test
    public void simpleStarter2() {
        StarterNode n1 = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        Starter s1 = new Starter() {
        };
        n1.registerStarter(s1);

        StarterNode n2 = new StarterNode(n1) {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };

        Assert.assertNotNull(n2.find(s1.getClass()));
    }

    @Test
    public void oneLevel() {
        StarterNode n = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        Starter s = new LocalStarter(0);
        n.registerStarter(s);
        Assert.assertNotNull(n.find(LocalStarter.class, 0));
    }

    @Test
    public void twoLevel() {
        StarterNode n1 = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        Starter s1 = new LocalStarter(0);
        n1.registerStarter(s1);

        StarterNode n2 = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        n2.setParent(n1);
        Starter s2 = new LocalStarter(1);
        n2.registerStarter(s2);

        Assert.assertNotNull(n2.find(LocalStarter.class, 1));
        Assert.assertNotNull(n2.find(LocalStarter.class, 0));
    }

    @Test
    public void testHostList1() {
        HostsList hl = new HostsList();
        StarterNode n2 = new StarterNode(hl) {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        StarterNode n3 = new StarterNode(n2) {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };

        Assert.assertEquals(hl, n3.getHostList());
    }

    @Test
    public void testHostList2() {
        StarterNode n2 = new StarterNode() {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        StarterNode n3 = new StarterNode(n2) {
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };

        HostsList hl = new HostsList();
        n2.setParent(hl);

        Assert.assertEquals(hl, n3.getHostList());
    }

}
