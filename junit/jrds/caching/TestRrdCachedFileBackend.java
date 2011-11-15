package jrds.caching;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.BackEndFactoryTest;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdRandomAccessFileBackendFactory;

public class TestRrdCachedFileBackend extends BackEndFactoryTest {
    static final private Logger logger = Logger.getLogger(TestRrdCachedFileBackend.class);
    static final private String FILENAME = "TestRrdCachedFileBackend";
    static final private RrdBackendFactory raffactory  = new RrdRandomAccessFileBackendFactory();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching", "jrds.caching.RrdCachedFileBackend", "jrds.caching.FilePage", "jrds.caching.PageCache");
        File libfile = new File(String.format("build/native.%s.%s", System.getProperty("os.name").replaceAll(" ", ""), System.getProperty("os.arch")));
        RrdCachedFileBackendFactory.loadDirect(libfile);
        RrdBackendFactory.registerFactory(RrdCachedFileBackendFactory.class);
        raffactory.start();
    }

    @Test
    public void read() throws IOException {
        File file = testFolder.newFile(FILENAME);
        FileOutputStream out = new FileOutputStream(file);
        String content = getClass().getCanonicalName();
        out.write(content.getBytes());
        PageCache pc =  new PageCache(10);
        RrdCachedFileBackend f = new RrdCachedFileBackend(file.getCanonicalPath(), true, pc);
        byte[] buffer = new byte[(int) (file.length() + 10)];
        f.read(0, buffer);
        Assert.assertEquals("content read mismatch", content, new String(buffer).trim());
    }

    @Test
    public void reopenRrd() throws IOException {
        File rrdFile = testFolder.newFile("testcached.rrd");
        if(rrdFile.exists())
            rrdFile.delete();

        RrdCachedFileBackendFactory factory = new RrdCachedFileBackendFactory();
        factory.setPageCache(10);
        factory.setSyncPeriod(30);
        factory.start();

        // first, define the RRD
        RrdDef rrdDef = new RrdDef(rrdFile.getCanonicalPath(), 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource(new DsDef("A", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addDatasource(new DsDef("B", DsType.DERIVE, 5l, 0, 10000));
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 10);
        rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 6, 20);

        // then, create a RrdDb from the definition and start adding data
        RrdDb rrdDb = new RrdDb(rrdDef, factory);
        long lastUpdate = rrdDb.getLastUpdateTime() + 1000;
        rrdDb.createSample().setAndUpdate(lastUpdate + ":1:2");
        String dump = rrdDb.dump();
        rrdDb.close();

        factory.stop();

        rrdDb = new RrdDb(rrdFile.getCanonicalPath(), raffactory);
        Assert.assertEquals("arc count mismatch", rrdDef.getArcCount(), rrdDb.getArcCount());
        Assert.assertEquals("ds count mismatch", rrdDef.getDsCount(), rrdDb.getDsCount());
        Assert.assertEquals("last update match", lastUpdate, rrdDb.getLastUpdateTime());
        //Try to read the whole file;
        try {
            String newdump = rrdDb.dump();
            Assert.assertEquals("dump hash code don't match", dump, newdump);
        } catch (Exception e) {
            Assert.fail("read whole file failed");
        }
    }

    @Test
    public void reopenRrd2() throws IOException {
        File rrdFile = testFolder.newFile("testcached.rrd");
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
        RrdDb rrdDb = new RrdDb(rrdDef, raffactory);
        long lastUpdate = rrdDb.getLastUpdateTime() + 1000;
        rrdDb.createSample().setAndUpdate(lastUpdate + ":1:2");
        String dump = rrdDb.dump();
        rrdDb.close();

        RrdCachedFileBackendFactory factory = new RrdCachedFileBackendFactory();
        factory.setPageCache(10);
        factory.setSyncPeriod(30);
        factory.start();

        rrdDb = new RrdDb(rrdFile.getCanonicalPath(), factory);
        Assert.assertEquals("arc count mismatch", rrdDef.getArcCount(), rrdDb.getArcCount());
        Assert.assertEquals("ds count mismatch", rrdDef.getDsCount(), rrdDb.getDsCount());
        Assert.assertEquals("last update match", lastUpdate, rrdDb.getLastUpdateTime());
        //Try to read the whole file;
        try {
            String newdump = rrdDb.dump();
            Assert.assertEquals("dump hash code don't match", dump, newdump);
        } catch (Exception e) {
            Assert.fail("read whole file failed");
        }

        factory.stop();
    }

    @Override
    @Test
    public void testName() {
        checkRegistred("CACHEDFILE", RrdCachedFileBackendFactory.class);        
    }

    @Override
    @Test
    public void testBeans() throws IntrospectionException {
        checkBeans(RrdCachedFileBackendFactory.class, "pageCache", "syncPeriod");
    }
}
