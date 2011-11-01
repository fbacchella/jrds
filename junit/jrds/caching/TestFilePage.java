package jrds.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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
    }

    @Test
    public void test1() throws IOException {
        File testFile = new File("tmp/testfilepage");
        if(testFile.exists())
            testFile.delete();
        ByteBuffer pagecacheBuffer = ByteBuffer.allocateDirect(1 * PageCache.PAGESIZE);
        FilePage page = new FilePage(pagecacheBuffer, 0);
        page.load(testFile, 0);
        page.write(0, getClass().getName().getBytes());
        page.sync();

        FileInputStream in = new FileInputStream(testFile);
        byte[] b = new byte[(int) testFile.length()];
        in.read(b);
        Assert.assertEquals("read does not match write", getClass().getName().trim(), new String(b).trim());
        testFile.delete();
    }

}
