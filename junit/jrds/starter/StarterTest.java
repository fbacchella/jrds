package jrds.starter;

import jrds.HostsList;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StarterTest {
	static class LocalStarter extends Starter {
		Object key = LocalStarter.class;
		LocalStarter(Object key) {
			this.key = key;
		}
		public static Object makeKey(jrds.starter.StarterNode node) {
			return LocalStarter.class;
		}
		/* (non-Javadoc)
		 * @see jrds.starter.Starter#getKey()
		 */
		@Override
		public Object getKey() {
			return key;
		}
	}
	static Logger logger = Logger.getLogger(StarterTest.class);

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();

		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {StarterNode.class.toString(), Starter.class.toString()}, logger.getLevel());
	}

	@Test
	public void simpleStarter() {
		StarterNode n = new StarterNode() { };
		Starter s = new Starter() {};
		n.registerStarter(s);
		
		Assert.assertNotNull(n.find(s.getClass()));
	}

	@Test
	public void simpleStarter2() {
		StarterNode n1 = new StarterNode() { };
		Starter s1 = new Starter() {};
		n1.registerStarter(s1);

		StarterNode n2 = new StarterNode(n1) { };

		Assert.assertNotNull(n2.find(s1.getClass()));
	}

	@Test
	public void oneLevel() {
		StarterNode n = new StarterNode() {
		};
		Starter s = new LocalStarter(0);
		n.registerStarter(s);
		Assert.assertNotNull(n.find(LocalStarter.class, 0));
	}

	@Test
	public void twoLevel() {
		StarterNode n1 = new StarterNode() {
		};
		Starter s1 = new LocalStarter(0);
		n1.registerStarter(s1);

		StarterNode n2 = new StarterNode() {
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
		StarterNode n2 = new StarterNode(hl) {};
		StarterNode n3 = new StarterNode(n2) {};
		
		Assert.assertEquals(hl, n3.getHostList());
	}

	@Test
	public void testHostList2() {
		StarterNode n2 = new StarterNode() {};
		StarterNode n3 = new StarterNode(n2) {};

		HostsList hl = new HostsList();
		n2.setParent(hl);
		
		Assert.assertEquals(hl, n3.getHostList());
	}

}
