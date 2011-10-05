package jrds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jrds.webapp.RolesACL;
import jrds.webapp.ACL.AdminACL;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPropertiesManager {
    static final private Logger logger = Logger.getLogger(TestPropertiesManager.class);
    static private final Random r = new Random();
    static private final String[] dirs = new String[] {"configdir", "rrddir", "tmpdir"};

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        System.out.println(System.getProperties().keySet());
        Tools.setLevel(logger, Level.TRACE, "jrds.PropertiesManager");
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
        File descpath = new File(System.getProperty("user.dir"), "desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());
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
        FileChannel tmpProp = new java.io.FileOutputStream("tmp/log4j.properties").getChannel();
        tmpProp.transferFrom(isChannel, 0, 4096);
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("log4jpropfile", "tmp/log4j.properties");
        pm.update();
        logger.debug("log file created");

        File logFile = new File("tmp/log4j.log");
        Assert.assertTrue("Log4j file not created", logFile.canRead());

    }
}
