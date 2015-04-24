import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.ArchivesSet;
import jrds.JrdsLoggerConfiguration;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Tools;
import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.GetMoke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;


public class TestUpgrade {
    static final private Logger logger = Logger.getLogger(TestUpgrade.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        JrdsLoggerConfiguration.configureLogger(TestUpgrade.class.getCanonicalName(), Level.DEBUG);
        Tools.setLevel(logger.getLevel(), "jrds" );
    }

    @Test
    public void bidule() throws Exception {

        ProbeDesc pd = GetMoke.getPd();
        pd.add("MokeDs2", DsType.GAUGE);

        @SuppressWarnings("unchecked")
        Probe<String,Number> p = GenerateProbe.quickProbe(testFolder);
        p.setPd(pd);
        p.setStep(300);
        p.setName("dummy");
        boolean checked = p.getMainStore().checkStoreFile(ArchivesSet.DEFAULT);
        Assert.assertTrue(checked);

        RrdDef def = ((RrdDb) p.getMainStore().getStoreObject()).getRrdDef();
        def.setStep(300);
        RrdDb db = new RrdDb(def);
        long time = System.currentTimeMillis() / 1000;
        Sample s = db.createSample();
        s.set(time + ":1:1");
        s.update();
        time +=60;
        s.set(time + ":2:2");
        s.update();
        time +=60;
        s.set(time + ":3:3");
        s.update();
        time +=60;
        s.set(time + ":4:4");
        s.update();
        time +=60;
        s.set(time + ":5:5");
        s.update();

        p.checkStore();
    }
}
