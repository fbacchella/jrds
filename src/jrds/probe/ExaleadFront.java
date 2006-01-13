package jrds.probe;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExaleadFront extends Exalead {
	static final private Logger logger = JrdsLogger.getLogger(ExaleadFront.class);
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("threads", ProbeDesc.GAUGE, "threads");
		pd.add("max", ProbeDesc.GAUGE, "max");
		pd.add("last-started", ProbeDesc.NONE);
		pd.setRrdName("exeafront");
		pd.setGraphClasses(new Object[] {});
		
	}
	/**
	 * @param monitoredHost
	 */
	public ExaleadFront(RdsHost monitoredHost) {
		super(monitoredHost, pd, 10000);
	}
}
