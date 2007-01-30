package jrds.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import jrds.ArgFactory;
import jrds.DescFactory;
import jrds.GraphFactory;
import jrds.ProbeFactory;
import jrds.PropertiesManager;

import org.junit.BeforeClass;
import org.junit.Test;

public class AllProbeCreation extends UnitTest{
	static Set<String> probeList;
	static ProbeFactory pf;
	@BeforeClass static public void configure() {
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
		probeList = df.getProbesDescMap().keySet();
	}
	@Test public void makeProbe() {
		System.out.println(probeList);
		for(String pName: probeList) {
			pf.makeProbe(pName, new ArrayList());
		}
	}

}
