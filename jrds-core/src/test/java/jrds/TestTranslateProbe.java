package jrds;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.mockobjects.MokeProbe;

public class TestTranslateProbe {

    static ProbeDesc<String> pd;

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, Exception {
        Tools.configure();
        Tools.prepareXml();
        pd = jrds.configuration.GeneratorHelper.getProbeDesc(Tools.parseRessource("fulldesc.xml"));

    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Probe");
    }

    @Test
    public void constructAndCheckImmediateHigh() {
        Probe<String, Number> p = new MokeProbe<String, Number>();
        p.setPd(pd);

        Map<String, String> nameMap = p.getCollectMapping();
        Map<String, Number> filteredSamples = new HashMap<String, Number>();
        Map<String, Number> resultSample = new HashMap<String, Number>();

        for(Map.Entry<String, Number> e: filteredSamples.entrySet()) {
            String dsName = nameMap.get(e.getKey());
            double value = e.getValue().doubleValue();
            if(dsName != null) {
                resultSample.put(dsName, value);
            } else {
                logger.debug("Dropped entry: " + e.getKey());
            }
        }
    }
}
