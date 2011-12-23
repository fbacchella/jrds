package jrds;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.configuration.ConfigObjectFactory;
import jrds.factories.ProbeFactory;
import jrds.mockobjects.DummyProbe;
import jrds.mockobjects.DummyProbeIndexed;
import jrds.mockobjects.DummyProbeIndexedUrl;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

public class AllProbeCreationTest {
    static final private Logger logger = Logger.getLogger(AllProbeCreationTest.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        StoreOpener.prepare("FILE");
        Tools.setLevel(logger, Level.TRACE, "jrds.Util");
    }

    @Test
    public void makeProbe() throws ParserConfigurationException, IOException, URISyntaxException, InvocationTargetException {
        PropertiesManager pm = new PropertiesManager();
        pm.setProperty("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("configdir", testFolder.getRoot().getCanonicalPath());
        pm.setProperty("autocreate", "true");
        pm.update();
        pm.libspath.clear();
        pm.strictparsing = true;
        pm.rrdbackend = "FILE";
        File descpath = new File("desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());

        Assert.assertTrue(pm.rrddir.isDirectory());

        ConfigObjectFactory conf = new ConfigObjectFactory(pm);

        Map<String, GraphDesc> graphDescMap = conf.setGraphDescMap();
        Map<String, ProbeDesc> probeDescMap = conf.setProbeDescMap();

        ProbeFactory pf = new ProbeFactory(probeDescMap, graphDescMap, pm);

        RdsHost host = new RdsHost("Empty");
        host.setHostDir(pm.rrddir);
        for(ProbeDesc pd: probeDescMap.values()) {
            logger.trace("Will create probedesc " + pd.getName());
            Probe<?,?> p = pf.makeProbe(pd.getName());
            for(PropertyDescriptor bean: pd.getBeans()) {
                try {
                    logger.trace(bean.getName() + " = " + bean.getReadMethod().invoke(p));
                } catch (IllegalArgumentException e) {
                    logger.error("bean read error for '" + bean.getName() + "': " + e.getMessage());
                } catch (IllegalAccessException e) {
                    logger.error("bean read error for '" + bean.getName() + "': " + e.getMessage());
                } catch (InvocationTargetException e) {
                    Throwable t = e;
                    while(t.getCause() != null)
                        t = t.getCause();
                    logger.error("bean read error for '" + bean.getName() + "': " + t);
                }
            }
        }
    }

}
