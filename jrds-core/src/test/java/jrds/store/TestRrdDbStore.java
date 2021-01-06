package jrds.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.data.DataProcessor;
import org.slf4j.event.Level;

import jrds.JrdsSample;
import jrds.Log4JRule;
import jrds.Probe;
import jrds.Tools;
import jrds.mockobjects.GenerateProbe;

public class TestRrdDbStore {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Probe");
    }

    @Test(timeout = 2000)
    public void testCreate() throws Exception {
        Probe<String, Number> p = GenerateProbe.quickProbe(testFolder, GenerateProbe.ChainedMap.start(0));
        p.getPd().add("test", DsType.COUNTER);
        Assert.assertTrue("Probe file creation failed", p.checkStore());
        try (Extractor e = p.getMainStore().getExtractor()) {
            e.addSource("test", "test");
            String[] dsNames = e.getNames();
            Assert.assertEquals("data source test not found", "test", dsNames[0]);
        };
    }

    @Test
    public void testCheckNoUpdate() throws Exception {
        Probe<String, Number> p = GenerateProbe.quickProbe(testFolder, GenerateProbe.ChainedMap.start(0));
        p.getPd().add("test", DsType.COUNTER);

        Assert.assertTrue("Probe file creation failed", p.checkStore());
        FileTime before = Files.getLastModifiedTime(Paths.get(p.getMainStore().getPath()));
        long sizeBefore = Files.size(Paths.get(p.getMainStore().getPath()));
        Thread.sleep(2000);

        // Change the path representation
        File hostdir = p.getHost().getHostDir();
        Path newdest = Files.createDirectory(Paths.get(hostdir.getCanonicalPath(), "sub"));
        p.getHost().setHostDir(Paths.get(newdest.toString(), "..").toFile());

        Assert.assertTrue("Probe file creation failed", p.checkStore());
        FileTime after = Files.getLastModifiedTime(Paths.get(p.getMainStore().getPath()));
        long sizeAfter = Files.size(Paths.get(p.getMainStore().getPath()));
        Assert.assertEquals(before, after);
        Assert.assertEquals(sizeBefore, sizeAfter);
    }

    @Test
    public void testStepChanged() throws Exception {
        Probe<String, Number> p = GenerateProbe.quickProbe(testFolder, GenerateProbe.ChainedMap.start(0));
        p.setStep(30);
        p.getPd().add("test1", DsType.COUNTER);
        Assert.assertTrue("Probe file creation failed", p.checkStore());
        p.setStep(60);
        p.getPd().add("test1", DsType.COUNTER);
        Assert.assertFalse("Probe file creation should have failed", p.checkStore());
    }

    @Test
    public void testCheckUpdate() throws Exception {
        Probe<String, Number> p = GenerateProbe.quickProbe(testFolder, GenerateProbe.ChainedMap.start(0));
        Path storePath = Paths.get(p.getMainStore().getPath());
        p.getPd().add("test1", DsType.COUNTER);
        Assert.assertTrue("Probe file creation failed", p.checkStore());
        long sizeBefore = Files.size(storePath);
        p.getPd().add("test2", DsType.COUNTER);
        Assert.assertTrue("Probe file creation failed", p.checkStore());
        long sizeAfter = Files.size(storePath);
        Assert.assertNotEquals(sizeBefore, sizeAfter);
    }

    @Test
    public void testFill() throws Exception {
        Probe<String, Number> p = GenerateProbe.quickProbe(testFolder);
        p.setStep(30);
        p.getPd().add("test", DsType.GAUGE);
        Assert.assertTrue("Probe file creation failed", p.checkStore());
        Extractor e = p.getMainStore().getExtractor();
        e.addSource("test", "test");
        String[] dsNames = e.getNames();
        Assert.assertEquals("data source test not found", "test", dsNames[0]);
        long start = p.getLastUpdate().getTime();
        for(int i = 1; i <= 30; i++) {
            JrdsSample s = p.newSample();
            long sampletime = i * p.getStep() * 1000 + start;
            sampletime = (sampletime) - (sampletime % (p.getStep() * 1000));
            s.setTime(new Date(sampletime));
            s.put("test", i);
            p.getMainStore().commit(s);
        }
        ExtractInfo ei = ExtractInfo.builder().interval(Instant.ofEpochMilli(start), Instant.ofEpochMilli(start + 30 * p.getStep() * 1000)).build();
        DataProcessor dp = p.extract(ei);
        double[][] values = dp.getValues();
        for(int i = 1; i <= 30; i++) {
            // Check raw values
            Assert.assertEquals("Wrong values stored", i, values[0][i], 1e-10);
            long sampletime = i * p.getStep() * 1000 + start;
            sampletime = (sampletime) - (sampletime % (p.getStep() * 1000));
        }
    }

}
