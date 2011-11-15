package jrds.caching;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFilePage {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);

    static final private File testFile = new File("tmp/testfilepage");

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
        File libfile = new File(String.format("build/native.%s.%s", System.getProperty("os.name").replaceAll(" ", ""), System.getProperty("os.arch")));
        RrdCachedFileBackendFactory.loadDirect(libfile);
    }

    @Before
    public void initialize() {
        if(testFile.exists())
            testFile.delete();        
    }
    
    @After
    public void finish() {
        if(testFile.exists() && ! logger.isTraceEnabled())
            testFile.delete();        
    }

    @Test
    public void testWrite() throws IOException {
        FilePage page = new FilePage(0);
        page.load(testFile, 0);
        page.write(0, getClass().getName().getBytes());
        page.free();

        FileInputStream in = new FileInputStream(testFile);
        byte[] b = new byte[(int) testFile.length()];
        in.read(b);
        Assert.assertEquals("read does not match write", getClass().getName().trim(), new String(b).trim());
    }

    @Test
    public void testRead() throws IOException {
        FileOutputStream in = new FileOutputStream(testFile);
        in.write(getClass().getName().getBytes());
        in.flush();
        in.close();
        
        FilePage page = new FilePage(0);
        page.load(testFile, 0);
        byte[] b = new byte[(int) testFile.length()];
        page.read(0, b);

        Assert.assertEquals("write does not match read", getClass().getName().trim(), new String(b).trim());
    }
    
    @Test(expected=IOException.class)
    public void testFailed() throws IOException {
        try {
            FilePage.prepare_fd(testFile.getCanonicalPath(), new FileDescriptor(), true);
        } catch (IOException e) {
            logger.debug("Caught exception: " + e.getMessage());
            throw e;
        }
    }

    @Test(expected=IOException.class)
    public void testFailed2() throws IOException {
        FileOutputStream out = new FileOutputStream(testFile);
        out.write(0);
        out.close();
        testFile.setReadOnly();
        try {
            FilePage.prepare_fd(testFile.getCanonicalPath(), new FileDescriptor(), false);
        } catch (IOException e) {
            logger.debug("Caught exception: " + e.getMessage());
            throw e;
        }
    }

}
