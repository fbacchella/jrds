package jrds.probe;

import jrds.ProbeDesc;
import jrds.RdsHost;

import org.rrd4j.DsType;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExaleadIndex extends Exalead {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("threads", DsType.GAUGE);
		pd.add("max", DsType.GAUGE);
		pd.add("ndocs", DsType.GAUGE);
		pd.add("search", DsType.COUNTER);
		pd.add("commands", DsType.COUNTER);
		pd.add("last-started");
		pd.setProbeName("exeaindex");
		pd.setGraphClasses(new Object[] {});
		
	}
	/**
	 * @param monitoredHost
	 */
	public ExaleadIndex(RdsHost monitoredHost) {
		super(monitoredHost, pd, 10011);
	}
}
