package jrds;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
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
        Tools.setLevel(logger, Level.DEBUG, "jrds.Util");
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
            Class<? extends Probe<?,?>> originalClass = pd.getProbeClass();
            if(jrds.probe.UrlProbe.class.isAssignableFrom(originalClass)) {
                pd.setProbeClass(DummyProbeIndexedUrl.class);
            }
            else if(jrds.probe.IndexedProbe.class.isAssignableFrom(originalClass)) {
                pd.setProbeClass(DummyProbeIndexed.class);
            }
            else
                pd.setProbeClass(DummyProbe.class);

            logger.trace("Will create probedesc " + pd.getName());
            Probe<?,?> p = pf.makeProbe(pd.getName());
            p.setHost(host);
            pf.configure(p, Collections.singletonList(originalClass));
            p.setLabel("mokelabel");
            if(p.checkStore()) {
                RrdDef def = p.getRrdDef();
                RrdDb db = new RrdDb(def);
                Sample s = db.createSample();
                s.update();
                db.close();
            }
            File rrdFile = new File(p.getRrdName());
            Assert.assertTrue(rrdFile + " not fond", rrdFile.exists());
            rrdFile.delete();
            //rrdFile.delete();
            /*for(GraphNode gn : p.getGraphList()) {
					Date now = new Date();
					Graph graph = new Graph(gn, new Date(now.getTime() - 10000000), now);
					graph.getPngBytes();
				}*/
            //} catch (Exception e) {
            //	logger.error("Exception thwron: ", e);
            //	Assert.fail("Probe failed for " + pd.getName() + ": " + e);
            //}
        }
    }

}
