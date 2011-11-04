package jrds.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPageCache {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);
    static final private int numpages = 4;
    static final private File testFile = new File("tmp/testpagecache");

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching", "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
        RrdCachedFileBackendFactory.loadDirect(new File("build/native"));
    }
    
    private void checkcontent() throws IOException {
        Assert.assertEquals("File size invalid", PageCache.PAGESIZE * numpages, testFile.length());
        FileInputStream in = new FileInputStream(testFile);
        byte[] bufferin = new byte[PageCache.PAGESIZE * numpages ];
        in.read(bufferin);
        for(int i=0 ; i < PageCache.PAGESIZE * numpages; i++ ) {
            Assert.assertEquals(String.format("Invalid content at offset %d", i), (byte)Math.floor(i / PageCache.PAGESIZE), bufferin[i]);
        }       
    }

    @Test
    public void fillSequential() throws IOException {
        PageCache pc = new PageCache(numpages,3600);
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = new byte[PageCache.PAGESIZE];
        for(byte i= 0; i < numpages; i++) {
            Arrays.fill(buffer, i);
            pc.write(testFile, i * PageCache.PAGESIZE, buffer);
        }
        pc.sync();
        checkcontent();
    }

    @Test
    public void fillReverse() throws IOException {
        PageCache pc = new PageCache(numpages,3600);
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = new byte[PageCache.PAGESIZE];
        for(byte i = (byte) (numpages - 1) ; i > 0; i--) {
            Arrays.fill(buffer, i);
            pc.write(testFile, i * PageCache.PAGESIZE, buffer);
        }
        pc.sync();
        checkcontent();
    }

    @Test
    public void fillOnce() throws IOException {
        PageCache pc = new PageCache(numpages,3600);
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = new byte[PageCache.PAGESIZE * numpages];
        for(byte i= 0; i < numpages; i++) {
            Arrays.fill(buffer, PageCache.PAGESIZE * i , PageCache.PAGESIZE * (i + 1), i);
        }
        pc.write(testFile, 0, buffer);
        pc.sync();
        checkcontent();
    }
    
    @Test
    public void readCountBigger() throws IOException {
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = new byte[PageCache.PAGESIZE * numpages];
        for(byte i = 0; i < numpages; i++) {
            Arrays.fill(buffer, PageCache.PAGESIZE * i , PageCache.PAGESIZE * (i + 1), (byte)(5 - i));
        }
        FileOutputStream out = new FileOutputStream(testFile);
        out.write(buffer);
        out.flush();
        out.close();
        Arrays.fill(buffer, (byte)0);
        PageCache pc = new PageCache(numpages / 2,3600);
        pc.read(testFile, 0, buffer);
        for(int i = 0; i < buffer.length; i++) {
            System.out.println("[" + i + "]=" + buffer[i]);
            //Assert.assertEquals("missmatch at offset " + i, (byte)(numpages + 1 -  Math.floor(i / PageCache.PAGESIZE)), buffer[i]);
        }
    }

    @Test
    public void fillBigger() throws IOException {
        PageCache pc = new PageCache(numpages / 2,3600);
        if(testFile.exists())
            testFile.delete();
        byte[] buffer = new byte[PageCache.PAGESIZE * numpages];
        for(byte i= 0; i < numpages; i++) {
            Arrays.fill(buffer, PageCache.PAGESIZE * i , PageCache.PAGESIZE * (i + 1), i);
        }
        pc.write(testFile, 0, buffer);
        pc.sync();
        checkcontent();
    }

}
