package jrds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.mockobjects.EmptyStoreFactory;
import jrds.webapp.ACL.AdminACL;
import jrds.webapp.RolesACL;

public class TestPropertiesManager {
    static private final String[] dirs = new String[] { "configdir", "rrddir", "tmpdir" };

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Rule
    public Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
    }
    
    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.PropertiesManager");
    }

    @Test
    public void testTmpDir() throws IOException {
        String oldtmpdirpath = System.getProperty("java.io.tmpdir");
        File newtmpdir = testFolder.newFile("tmp");
        newtmpdir.delete();
        newtmpdir.mkdir();
        System.setProperty("java.io.tmpdir", newtmpdir.getPath());
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.update();
        System.setProperty("java.io.tmpdir", oldtmpdirpath);
        File jrdstmpdir = new File(newtmpdir, "jrds");
        Assert.assertEquals(jrdstmpdir.getCanonicalPath(), pm.tmpdir.getCanonicalPath());
        Assert.assertTrue("tmpdir should have been created", newtmpdir.exists());
    }

    @Test
    public void testConfigAutoCreate() throws IOException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
        PropertiesManager pm = new PropertiesManager();

        Map<String, File> dirMap = new HashMap<String, File>(dirs.length);
        for(String dirname: dirs) {
            File dir = testFolder.newFolder(dirname);
            pm.setProperty(dirname, dir.getPath());
            dirMap.put(dirname, dir);
        }
        pm.setProperty("autocreate", "true");
        pm.update();

        // Match the given name and was created
        for(Map.Entry<String, File> e: dirMap.entrySet()) {
            File dir = e.getValue();
            Assert.assertEquals(dir.getPath(), pm.getClass().getDeclaredField(e.getKey()).get(pm).toString());
            Assert.assertTrue(dir.isDirectory());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigNoAutoCreate() throws IOException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
        PropertiesManager pm = new PropertiesManager();

        Map<String, File> dirMap = new HashMap<String, File>(dirs.length);
        for(String dirname: dirs) {
            File dir = testFolder.newFolder(dirname);
            dir.delete();
            pm.setProperty(dirname, dir.getPath());
            dirMap.put(dirname, dir);
        }
        pm.setProperty("autocreate", "false");
        pm.update();
    }

    @Test
    public void testSecurity() throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("security", "true");
        pm.setProperty("adminrole", "role1");
        pm.setProperty("defaultroles", " role2 ,role3");
        pm.update();

        Assert.assertEquals("Bad default ACL class", RolesACL.class, pm.defaultACL.getClass());
        RolesACL rolesacl = (RolesACL) pm.defaultACL;
        Assert.assertTrue("Admin role1 not found", rolesacl.getRoles().contains("role1"));
        Assert.assertTrue("default role role2 not found", rolesacl.getRoles().contains("role2"));
        Assert.assertTrue("default role role3 not found", rolesacl.getRoles().contains("role3"));

        Assert.assertEquals("Bad adminACL", AdminACL.class, pm.adminACL.getClass());
        AdminACL adminacl = (AdminACL) pm.adminACL;
        Assert.assertEquals("Bad admin role", "role1", adminacl.getAdminRole());
    }

    @Test
    public void testlog4jpropfile() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("testpmlog4j.properties")) {
            Properties log4jprops = new Properties();
            log4jprops.load(is);
            log4jprops.setProperty("log4j.appender.A2.file", testFolder.newFile().getAbsolutePath());
            File log4jpropsdest = testFolder.newFile("testpmlog4j.properties");
            log4jpropsdest.delete();
            try (FileOutputStream fos = new FileOutputStream(log4jpropsdest)) {
                log4jprops.store(fos, "");
                PropertiesManager pm = new PropertiesManager();
                pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
                pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
                pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
                pm.setProperty("log4jpropfile", log4jpropsdest.getCanonicalPath());
                pm.update();
                logger.debug("log file created");
                Assert.assertTrue("Log4j file not created", log4jpropsdest.canRead());
            }
        }
    }

    @Test
    public void testDefaultStore() throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.update();
        pm.configureStores();
        Assert.assertEquals("Default store configuration failed", jrds.store.RrdDbStoreFactory.class, pm.defaultStore.getClass());
    }

    @Test
    public void testDefaultStoreEmpty() throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("storefactory", jrds.store.CacheStoreFactory.class.getCanonicalName());
        pm.update();
        pm.configureStores();
        Assert.assertEquals("Default store configuration failed", jrds.store.CacheStoreFactory.class, pm.defaultStore.getClass());
    }

    @Test
    public void testDefaultStoreList() throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("stores", "cache");
        pm.setProperty("rrdbackend", "NIO");
        pm.setProperty("store.cache.factory", EmptyStoreFactory.class.getCanonicalName());
        pm.update();
        pm.configureStores();
        Assert.assertEquals("Addition store configuration failed", 1, pm.stores.size());
        Assert.assertEquals("Addition store configuration failed", EmptyStoreFactory.class, pm.stores.get("cache").getClass());
    }

}
