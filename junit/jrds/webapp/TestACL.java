package jrds.webapp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jrds.TestPropertiesManager;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestACL {
	static final private Logger logger = Logger.getLogger(TestPropertiesManager.class);

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.webapp.ACL"}, logger.getLevel());
	}
	
	private ParamsBean prepareMoke(String[] keys, Object[] values) {
		Map<String, Object> paramsargs = new HashMap<String, Object>(keys.length);
		for(int i=0; i< keys.length; i++) {
			paramsargs.put(keys[i], values[0]);
		}
		return new MockParamsBean(paramsargs);		
	}
	
	private <T> Set<T> arrayToSet(T... args) {
		Set<T> set = new HashSet<T>(args.length);
		set.addAll(Arrays.asList(args));
		return set;
	}
	
	@Test
	public void testAllowed() {
		ParamsBean params = prepareMoke(new String[] {"roles"}, new Object[] {arrayToSet("role1")});		
		ACL acl = new RolesACL(arrayToSet("role1"));
		Assert.assertTrue("Access should have been allowed", acl.check(params));
	}
	@Test
	public void testDenied() {
		ParamsBean params = prepareMoke(new String[] {"roles"}, new Object[] {arrayToSet("role2")});		
		ACL acl = new RolesACL(arrayToSet("role1"));
		Assert.assertFalse("Access should have been allowed", acl.check(params));
	}

}
