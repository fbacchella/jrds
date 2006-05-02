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
public class ExaleadFront extends Exalead {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("threads", DsType.GAUGE, "threads");
		pd.add("max", DsType.GAUGE, "max");
		pd.add("last-started");
		pd.setProbeName("exeafront");
		pd.setGraphClasses(new Object[] {});
		
	}
	/**
	 * @param monitoredHost
	 */
	public ExaleadFront(RdsHost monitoredHost) {
		super(monitoredHost, pd, 10000);
	}
}
