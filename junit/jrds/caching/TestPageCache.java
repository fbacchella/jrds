package jrds.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

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

}
