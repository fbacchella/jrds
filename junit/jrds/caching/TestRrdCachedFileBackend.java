package jrds.caching;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;

public class TestRrdCachedFileBackend {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);
    static final private RrdCachedFileBackendFactory factory = new RrdCachedFileBackendFactory();
    {
        RrdCachedFileBackendFactory.setPageCache(10, 30);
    }


    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching", "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
        RrdCachedFileBackendFactory.loadDirect(new File("build/native"));
    }

    @Test
    public void test1() throws IOException {
        PageCache pc =  new PageCache(100, 3600);
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
        File rrdFile = new File("tmp/testcached.rrd");
        if(rrdFile.exists())
            rrdFile.delete();
        // first, define the RRD
        RrdDef rrdDef = new RrdDef(rrdFile.getCanonicalPath(), 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource(new DsDef("A", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addDatasource(new DsDef("B", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 10);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 6, 20);

        // then, create a RrdDb from the definition and start adding data
        RrdDb rrdDb = new RrdDb(rrdDef, factory);
        rrdDb.createSample().setAndUpdate((rrdDb.getLastUpdateTime() + 1000) + ":1:2");
        rrdDb.close();

        factory.sync();

        rrdDb = new RrdDb(rrdFile.getCanonicalPath(), new RrdRandomAccessFileBackendFactory());
        Assert.assertEquals("arc count mismatch", rrdDef.getArcCount(), rrdDb.getArcCount());
        Assert.assertEquals("ds count mismatch", rrdDef.getDsCount(), rrdDb.getDsCount());
        int hash = rrdDb.dump().hashCode();
        long lastUpdate = rrdDb.getLastUpdateTime();

        rrdDb = new RrdDb(rrdFile.getCanonicalPath(), factory);
        Assert.assertEquals("arc count mismatch", rrdDef.getArcCount(), rrdDb.getArcCount());
        Assert.assertEquals("ds count mismatch", rrdDef.getDsCount(), rrdDb.getDsCount());
        Assert.assertEquals("last update mismatch", lastUpdate, rrdDb.getLastUpdateTime());
        Assert.assertEquals("string dump mismatch", hash, rrdDb.dump().hashCode());

    }
}
