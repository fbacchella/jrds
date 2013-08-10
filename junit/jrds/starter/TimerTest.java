package jrds.starter;

import java.io.IOException;

import jrds.PropertiesManager;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TimerTest {
    static Logger logger = Logger.getLogger(TimerTest.class);
    
    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();

        logger.setLevel(Level.TRACE);
        Tools.setLevel(new String[] {StarterNode.class.toString(), Starter.class.toString()}, logger.getLevel());
    }
    
    @Test
    public void buildDefault() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "timeout=1", "step=5");
        Timer t = new Timer(Timer.DEFAULTNAME, pm.timers.get(Timer.DEFAULTNAME));
        Assert.assertEquals("bad timeout", 1, t.getTimeout());
        Assert.assertEquals("bad step", 5, t.getStep());
    }

    @Test
    public void buildOther() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "timeout=1", "step=5", "timers=slow", "timer.slow.timeout=30", "timer.slow.step=3600");
        Timer t = new Timer("slow", pm.timers.get("slow"));
        Assert.assertEquals("bad timeout", 30, t.getTimeout());
        Assert.assertEquals("bad step", 3600, t.getStep());
    }

}
