package jrds.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestFilePage {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
        RrdCachedFileBackendFactory.loadDirect(new File("build/native"));
    }

    @Test
    public void testWrite() throws IOException {
        File testFile = new File("tmp/testfilepage");
        if(testFile.exists())
            testFile.delete();
        
        FilePage page = new FilePage(0);
        page.load(testFile, 0);
        page.write(0, getClass().getName().getBytes());
        page.free();

        FileInputStream in = new FileInputStream(testFile);
        byte[] b = new byte[(int) testFile.length()];
        in.read(b);
        Assert.assertEquals("read does not match write", getClass().getName().trim(), new String(b).trim());
        testFile.delete();
    }

    @Test
    public void testRead() throws IOException {
        File testFile = new File("tmp/testfilepage");
        if(testFile.exists())
            testFile.delete();
        
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

}
