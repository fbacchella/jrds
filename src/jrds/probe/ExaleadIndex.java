package jrds.probe;

import jrds.ProbeDesc;
import jrds.RdsHost;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExaleadIndex extends Exalead {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("threads", ProbeDesc.GAUGE);
		pd.add("max", ProbeDesc.GAUGE);
		pd.add("ndocs", ProbeDesc.GAUGE);
		pd.add("search", ProbeDesc.COUNTER);
		pd.add("commands", ProbeDesc.COUNTER);
		pd.add("last-started", ProbeDesc.NONE);
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
