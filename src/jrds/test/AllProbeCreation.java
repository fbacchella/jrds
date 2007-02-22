package jrds.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import jrds.ArgFactory;
import jrds.DescFactory;
import jrds.GraphFactory;
import jrds.HostsList;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.ProbeFactory;
import jrds.PropertiesManager;
import jrds.RdsGraph;
import jrds.RdsHost;

import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdMemoryBackendFactory;
import org.rrd4j.core.Sample;
import org.junit.BeforeClass;
import org.junit.Test;

public class AllProbeCreation/* extends UnitTest*/ {
	 static Collection<ProbeDesc> probeList;
	 static ProbeFactory pf;
	 static RrdMemoryBackendFactory membef;
	 @BeforeClass public static void prepare() {
			RrdBackendFactory.setDefaultFactory("MEMORY");
			//.registerAndSetAsDefaultFactory(new org.jrobin.core.RrdMemoryBackendFactory());
			membef = (RrdMemoryBackendFactory) RrdBackendFactory.getDefaultFactory();
	 }
	 
	@BeforeClass static public void configure() {
		HostsList hl = HostsList.getRootGroup();
		hl.configure(new PropertiesManager());
		UnitTest.configure();
		PropertiesManager pm = new PropertiesManager();
		ArgFactory af= new ArgFactory();
		DescFactory df = new DescFactory(af);
		GraphFactory gf = new GraphFactory(df.getGraphDescMap(), true);
		pf = new ProbeFactory(df.getProbesDescMap(), gf, pm, true);

		try {
			df.importDescUrl(DescFactory.class.getResource("/probe"));
			df.importDescUrl(DescFactory.class.getResource("/graph"));
		} catch (IOException e) {
		}
		probeList = df.getProbesDescMap().values();
	}
	@Test public void makeProbe() throws ParserConfigurationException, IOException {
		RdsHost host = new RdsHost("Empty");
		for(ProbeDesc pd: probeList) {
			Class originalClass = pd.getProbeClass();
			if(jrds.probe.IndexedProbe.class.isAssignableFrom(originalClass)) {
				pd.setProbeClass(DummyProbeIndexed.class);
			}
			else
				pd.setProbeClass(DummyProbe.class);
			Probe p = pf.makeProbe(pd.getName(), Collections.singletonList(originalClass));
			p.setHost(host);
			p.checkStore();
			RrdDef def = p.getRrdDef();
			RrdDb db = new RrdDb(def);
			Sample s = db.createSample();
			s.update();
			db.close();
			for(RdsGraph graph : p.getGraphList()) {
				graph.getGraphDesc();
				Date now = new Date();
				graph.getPngBytes(new Date(now.getTime() - 10000000), now);
			}
			membef.delete(p.getRrdName());
		}
	}

}
