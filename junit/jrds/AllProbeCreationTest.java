package jrds;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.factories.ConfigObjectFactory;
import jrds.factories.GraphFactory;
import jrds.factories.Loader;
import jrds.factories.ProbeFactory;
import jrds.mockobjects.DummyProbe;
import jrds.mockobjects.DummyProbeIndexed;
import jrds.mockobjects.DummyProbeIndexedUrl;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

public class AllProbeCreationTest {
	static final private Logger logger = Logger.getLogger(AllProbeCreationTest.class);

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		logger.setLevel(Level.ERROR);
		Tools.setLevel(new String[] {"jrds.Util"}, logger.getLevel());
	}

	@Test
	public void makeProbe() throws ParserConfigurationException, IOException {
		PropertiesManager pm = new PropertiesManager();
		pm.setProperty("rrddir", "tmp");
		pm.setProperty("tmpdir", "tmp");
		pm.setProperty("configdir", "tmp");
		pm.setProperty("autocreate", "true");
		pm.update();
		
		Assert.assertTrue(pm.rrddir.isDirectory());
		//pm.rrdbackend = "MEM";

		Loader l = new Loader();
		l.importDir(new File("desc"));
		ConfigObjectFactory conf = new ConfigObjectFactory(pm);

		Map<String, GraphDesc> graphDescMap = conf.setGraphDescMap(l.getRepository(Loader.ConfigType.GRAPHDESC));
		Map<String, ProbeDesc> probeDescMap = conf.setProbeDescMap(l.getRepository(Loader.ConfigType.PROBEDESC));

		GraphFactory gf = new GraphFactory(graphDescMap, pm.legacymode);
		ProbeFactory pf = new ProbeFactory(probeDescMap, gf, pm);

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
			Probe p = pf.makeProbe(pd.getName(), host, Collections.singletonList(originalClass));
			p.setLabel("mokelabel");
			if(p.checkStore()) {
				RrdDef def = p.getRrdDef();
				RrdDb db = new RrdDb(def);
				Sample s = db.createSample();
				s.update();
				db.close();
			}
			File rrdFile = new File(p.getRrdName());
			Assert.assertTrue(rrdFile.exists());
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
