package jrds;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.xml.parsers.ParserConfigurationException;

import jrds.probe.DummyProbe;
import jrds.probe.DummyProbeIndexed;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

public class AllProbeCreation extends JrdsTester {
	static Collection<ProbeDesc> probeList;
	 static ProbeFactory pf;

	 @BeforeClass static public void configure() {
		JrdsTester.configure();
		
		//HostsList hl = HostsList.getRootGroup();
		PropertiesManager pm = new PropertiesManager();
		//hl.configure(pm);
		//UnitTest.configure();
		pm.rrdbackend = "MEM";
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
			Class<? extends Probe> originalClass = pd.getProbeClass();
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
			/*for(GraphNode gn : p.getGraphList()) {
				Date now = new Date();
				Graph graph = new Graph(gn, new Date(now.getTime() - 10000000), now);
				graph.getPngBytes();
			}*/
		}
	}

}
