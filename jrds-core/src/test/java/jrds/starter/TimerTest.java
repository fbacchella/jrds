package jrds.starter;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tools;

public class TimerTest {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, StarterNode.class.getCanonicalName(), Starter.class.getCanonicalName());
    }

    @Test
    public void buildDefault() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "timeout=1", "step=5", "slowcollecttime=1");
        Timer t = new Timer(Timer.DEFAULTNAME, pm.timers.get(Timer.DEFAULTNAME));
        Assert.assertEquals("bad timeout", 1, t.getTimeout());
        Assert.assertEquals("bad step", 5, t.getStep());
        Assert.assertEquals("bad slow collect time", 1, t.getSlowCollectTime());
    }

    @Test
    public void buildOther() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "timeout=1", "step=5", "timers=slow", "timer.slow.timeout=30", "timer.slow.step=3600", "timer.slow.slowcollecttime=15");
        Timer t = new Timer("slow", pm.timers.get("slow"));
        Assert.assertEquals("bad timeout", 30, t.getTimeout());
        Assert.assertEquals("bad step", 3600, t.getStep());
        Assert.assertEquals("bad slow collect time", 15, t.getSlowCollectTime());
    }

}
