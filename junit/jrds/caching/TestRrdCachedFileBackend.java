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
        Tools.setLevel(logger, Level.TRACE);
    }
    
    @Test
    public void test1() throws IOException {
        String libname = System.mapLibraryName("direct");
        File cwd =  new File(".");
        File nativedir = new File(new File(new File(cwd,"build"), "native"), libname);
        Runtime.getRuntime().load(nativedir.getCanonicalPath());
        RrdCachedFileBackend f = new RrdCachedFileBackend(nativedir.getCanonicalPath(), true);
        f.read(0, new byte[100]);
    }
}
