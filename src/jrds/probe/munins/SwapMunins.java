/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.SwapIO;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SwapMunins extends MuninsProbe {
	static private final Logger logger = Logger.getLogger(SwapMunins.class);

	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("swapIn", DsType.COUNTER, "swap_in.value");
		pd.add("swapOut", DsType.COUNTER, "swap_out.value");
		pd.setGraphClasses(new Class[] {SwapIO.class});
		pd.setMuninsProbesNames(new String[] { "swap_munins" });
		pd.setProbeName("swap_munins");
	}

	/**
	 * @param monitoredHost
	 */
	public SwapMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
