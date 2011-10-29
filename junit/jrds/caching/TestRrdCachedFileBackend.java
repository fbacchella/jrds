package jrds.caching;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRrdCachedFileBackend {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
    }
    
    @Test
    public void test1() throws IOException {
        PageCache pc =  new PageCache(100, 30);
        String libname = System.mapLibraryName("direct");
        File cwd =  new File(".");
        File nativedir = new File(new File(new File(cwd,"build"), "native"), libname);
        Runtime.getRuntime().load(nativedir.getCanonicalPath());
        RrdCachedFileBackend f = new RrdCachedFileBackend(new File("/tmp/passwd").getCanonicalPath(), true, pc);
        byte[] buffer = new byte[1000];
        f.read(1, buffer);
        logger.debug(new String(buffer));
    }
}
