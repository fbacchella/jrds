package jrds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

import jrds.webapp.ACL.AdminACL;
import jrds.webapp.RolesACL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPropertiesManager {
    static final private Logger logger = Logger.getLogger(TestPropertiesManager.class);
    static private final String[] dirs = new String[] {"configdir", "rrddir", "tmpdir"};

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        System.out.println(System.getProperties().keySet());
        Tools.setLevel(logger, Level.TRACE, "jrds.PropertiesManager");
    }

    @Test
    public void testTmpDir() throws IOException {
        String oldtmpdirpath = System.getProperty("java.io.tmpdir");
        File newtmpdir = testFolder.newFile("tmp");
        newtmpdir.delete();
        System.setProperty("java.io.tmpdir", newtmpdir.getPath());
        PropertiesManager pm = new PropertiesManager();
        System.setProperty("java.io.tmpdir", oldtmpdirpath);
        File jrdstmpdir = new File(newtmpdir, "jrds");
        Assert.assertEquals(jrdstmpdir, pm.tmpdir);
        Assert.assertFalse("tmpdir should not be created", newtmpdir.exists());
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

        //Match the given name and was created
        for(Map.Entry<String, File> e: dirMap.entrySet()) {
            File dir = e.getValue();
            Assert.assertEquals(dir.getPath(), pm.getClass().getDeclaredField(e.getKey()).get(pm).toString());
            Assert.assertTrue(dir.isDirectory());
        }
    }

    @Test
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

        Assert.assertEquals("tmp/jrds", pm.tmpdir.toString());
        Assert.assertNull(pm.configdir);
        Assert.assertNull(pm.rrddir);

        //None was created
        for(Map.Entry<String, File> e: dirMap.entrySet()) {
            File dir = e.getValue();
            Assert.assertFalse(dir.exists());
        }
    }

    @Test
    public void testSecurity() {
        PropertiesManager pm = new PropertiesManager();
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
        InputStream is = Tools.class.getResourceAsStream("/ressources/log4j.properties");
        ReadableByteChannel isChannel = Channels.newChannel(is);
        File log4jprops = testFolder.newFile("log4j.properties");
        FileChannel tmpProp = new java.io.FileOutputStream(log4jprops).getChannel();
        tmpProp.transferFrom(isChannel, 0, 4096);
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("log4jpropfile", log4jprops.getCanonicalPath());
        pm.update();
        logger.debug("log file created");

        File logFile = new File("tmp/log4j.log");
        Assert.assertTrue("Log4j file not created", logFile.canRead());
        logFile.delete();
    }
    
}
