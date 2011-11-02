package jrds.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.Util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPageCache {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
    }

    @Test
    public void test1() throws IOException {
        String outString = getClass().getName();
        PageCache pc = new PageCache(4,3600);
        File testFile = new File("tmp/testpagecache");
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = outString.getBytes();
        pc.write(testFile, 0, buffer);
        pc.sync();

        FileInputStream in = new FileInputStream(testFile);
        byte[] b = new byte[(int) testFile.length()];
        in.read(b);
        Assert.assertEquals("read does not match write", outString.trim(), new String(b).trim());
        testFile.delete();
    }
    
    @Test
    public void test2() throws IOException {
        int numpages = 3;
        PageCache pc = new PageCache(numpages,3600);
        File testFile = new File("tmp/testpagecache");
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = new byte[PageCache.PAGESIZE];
        for(byte i= 0; i < numpages; i++) {
            Arrays.fill(buffer, i);
            pc.write(testFile, i * PageCache.PAGESIZE, buffer);
        }
        pc.sync();
        Assert.assertEquals("File size invalid", PageCache.PAGESIZE * numpages, testFile.length());
        FileInputStream in = new FileInputStream(testFile);
        byte[] bufferin = new byte[PageCache.PAGESIZE * numpages ];
        in.read(bufferin);
        for(int i=0 ; i < PageCache.PAGESIZE * numpages; i++ ) {
            Assert.assertEquals(String.format("Invalid content at offset %d", i), (byte)Math.floor(i / PageCache.PAGESIZE), bufferin[i]);
        }

    }

}
