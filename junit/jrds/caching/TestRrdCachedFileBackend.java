package jrds.caching;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;

public class TestRrdCachedFileBackend {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);
    static final private RrdCachedFileBackendFactory factory = new RrdCachedFileBackendFactory();
    {
        RrdCachedFileBackendFactory.setPageCache(10, 30);
    }


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

    @Test
    public void reopen() throws IOException {
        String rrdPath = "tmp/testcached.rrd";
        // first, define the RRD
        RrdDef rrdDef = new RrdDef(rrdPath, 300);
        rrdDef.addDatasource(new DsDef("toto", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 600); // 1 step, 600 rows
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 6, 700); // 6 steps, 700 rows
        rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, 600);

        // then, create a RrdDb from the definition and start adding data
        RrdDb rrdDb = new RrdDb(rrdDef, factory);
        rrdDb.close();
        
        factory.sync();
        
        rrdDb = new RrdDb(rrdPath);

    }
}
