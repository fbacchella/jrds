import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.mockobjects.DummyProbe;
import jrds.mockobjects.GetMoke;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;


public class TestUpgrade {
	static final private Logger logger = Logger.getLogger(TestUpgrade.class);

	
	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.setLevel(new String[] {"jrds"}, logger.getLevel());
	}

	@Test
	public void bidule() throws Exception {
		ProbeDesc pd = GetMoke.getPd();
		pd.add("MokeDs2", DsType.GAUGE);

		Probe<?,?> p = new DummyProbe() {
			protected ArcDef[] getArcDefs() {
				ArcDef[] defaultArc = new ArcDef[1];
				defaultArc[0] = new ArcDef(ConsolFun.AVERAGE, 0.5, 1, 10);
				return defaultArc;
			}

			/* (non-Javadoc)
			 * @see jrds.Probe#getRrdName()
			 */
			@Override
			public String getRrdName() {
				return "tmp/test.rrd";
			}
		};

		p.setPd(pd);
		
		File f = new File("tmp/test.rrd");
		f.delete();
		RrdDef def = p.getRrdDef();
		def.setStep(300);
		RrdDb db = new RrdDb(def);
		long time = System.currentTimeMillis() / 1000;
		Sample s = db.createSample();
		s.set(time + ":1:1");
		s.update();
		time +=60;
		s.set(time + ":2:2");
		s.update();
		time +=60;
		s.set(time + ":3:3");
		s.update();
		time +=60;
		s.set(time + ":4:4");
		s.update();
		time +=60;
		s.set(time + ":5:5");
		s.update();
		
		p.checkStore();
	}
}
