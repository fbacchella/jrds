package jrds.probe;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import jrds.Log4JRule;
import jrds.Probe;
import jrds.Tools;
import jrds.factories.ProbeMeta;
import jrds.probe.munin.MuninDiscoverAgent;
import jrds.webapp.DiscoverAgent;

public class TestMeta {

    @ProbeMeta(discoverAgent = MuninDiscoverAgent.class)
    public abstract class DummyProbe1<A, B> extends Probe<A, B> {

    }

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Util");
    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Test
    public void build1() throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Assert.assertTrue(DummyProbe1.class.isAnnotationPresent(ProbeMeta.class));
        ProbeMeta meta = DummyProbe1.class.getAnnotation(ProbeMeta.class);
        logger.debug(meta.discoverAgent());
        DiscoverAgent da = meta.discoverAgent().getConstructor().newInstance();
        Assert.assertNotNull("a discover agent can't be build", da);
    }
}
