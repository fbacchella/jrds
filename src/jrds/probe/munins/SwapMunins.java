/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.SwapIO;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SwapMunins extends MuninsProbe {
	static private final Logger logger = JrdsLogger.getLogger(SwapMunins.class.getPackage().getName());

	static final private ProbeDesc pd = new ProbeDesc(2);
	static {
		pd.add("swapIn", ProbeDesc.COUNTER, "swap_in.value");
		pd.add("swapOut", ProbeDesc.COUNTER, "swap_out.value");
		pd.setGraphClasses(new Class[] {SwapIO.class});
		pd.setMuninsProbesNames(new String[] { "swap_munins" });
		pd.setRrdName("swap_munins");
	}

	/**
	 * @param monitoredHost
	 */
	public SwapMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
