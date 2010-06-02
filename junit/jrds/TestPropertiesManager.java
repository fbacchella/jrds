package jrds;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPropertiesManager {
	static final private Logger logger = Logger.getLogger(TestPropertiesManager.class);
	static private final Random r= new Random();
	static private final String[] dirs = new String[] {"configdir", "rrddir", "tmpdir"};

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.PropertiesManager"}, logger.getLevel());
		System.setProperty("java.io.tmpdir",  "tmp");
	}
	
	@Test
	public void testEmpty() {
		String oldtmpdirpath = System.getProperty("java.io.tmpdir");
		File newtmpdir = new File(System.getProperty("java.io.tmpdir"), "jrds" + r.nextInt());;
		System.setProperty("java.io.tmpdir", newtmpdir.getPath());
		PropertiesManager pm = new PropertiesManager();
		System.setProperty("java.io.tmpdir", oldtmpdirpath);
		File jrdstmpdir = new File(newtmpdir, "jrds");
		Assert.assertEquals(jrdstmpdir, pm.tmpdir);
		Assert.assertFalse(newtmpdir.exists());
		Assert.assertFalse(jrdstmpdir.exists());
		Assert.assertNull(pm.rrddir);
		Assert.assertNull(pm.configdir);
	}

	@Test
	public void testConfig1() {
		File tmpdir = new File(System.getProperty("java.io.tmpdir"),"jrds"); 
		boolean toclean = tmpdir.isDirectory();
		PropertiesManager pm = new PropertiesManager();
		pm.update();
		Assert.assertEquals(tmpdir, pm.tmpdir);
		Assert.assertTrue(tmpdir.isDirectory());
		if(toclean)
			tmpdir.delete();
		Assert.assertNull(pm.rrddir);
		Assert.assertNull(pm.configdir);
	}
	
	@Test
	public void testConfig2() throws IOException {
		PropertiesManager pm = new PropertiesManager();

		Map<String, File> dirMap = new HashMap<String, File>(dirs.length);
		for(String dirname: dirs) {
			File dir = new File(System.getProperty("java.io.tmpdir"), "jrds" + r.nextInt());;
			pm.setProperty(dirname, dir.getPath());
			dirMap.put(dirname, dir);
		}
		pm.setProperty("autocreate", "true");
		pm.update();
		
		Assert.assertEquals(dirMap.get("tmpdir"), pm.tmpdir);
		Assert.assertEquals(dirMap.get("configdir"), pm.configdir);
		Assert.assertEquals(dirMap.get("rrddir"), pm.rrddir);

		for(File dir : dirMap.values()) {
			Assert.assertTrue(dir.isDirectory());
			dir.delete();
		}
	}

	@Test
	public void testConfig3() throws IOException {
		File tmpdir = new File(System.getProperty("java.io.tmpdir"),"jrds"); 
		PropertiesManager pm = new PropertiesManager();

		Map<String, File> dirMap = new HashMap<String, File>(dirs.length);
		for(String dirname: dirs) {
			File dir = new File(System.getProperty("java.io.tmpdir"), "jrds" + r.nextInt());;
			pm.setProperty(dirname, dir.getPath());
			dirMap.put(dirname, dir);
		}
		dirMap.put("tmpdir", tmpdir);
		pm.setProperty("autocreate", "false");
		pm.update();
		
		Assert.assertEquals(dirMap.get("tmpdir"), pm.tmpdir);
		Assert.assertEquals(null, pm.configdir);
		Assert.assertEquals(null, pm.rrddir);

		Assert.assertTrue(tmpdir.exists());
		for(File dir : dirMap.values()) {
			logger.trace(dir);
			Assert.assertTrue(dir.equals(tmpdir) || ! dir.exists());
		}
	}

}
