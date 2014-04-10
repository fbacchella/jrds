package jrds.probe;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jrds.Probe;
import jrds.Tools;
import jrds.factories.ProbeMeta;
import jrds.snmp.SnmpDiscoverAgent;
import jrds.webapp.DiscoverAgent;

public class TestMeta{
    @ProbeMeta(
            discoverAgent = SnmpDiscoverAgent.class
    )
    public abstract class DummyProbe1<A,B> extends Probe<A,B> {
        
    }

    static final private Logger logger = Logger.getLogger(TestMeta.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        logger.setLevel(Level.DEBUG);
        Tools.setLevel(new String[] {"jrds.Util"}, logger.getLevel());
    }

    
    @Test
    public void build1() throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Assert.assertTrue(DummyProbe1.class.isAnnotationPresent(ProbeMeta.class));
        ProbeMeta  meta = DummyProbe1.class.getAnnotation(ProbeMeta.class);
        logger.debug(meta.discoverAgent());
        DiscoverAgent da = meta.discoverAgent().getConstructor().newInstance();
        Assert.assertNotNull("a discover agent can't be build", da);
    }
}
