/*
 * Created on 11 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.JrdsLogger;
import jrds.MuninsProbe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.CpuRawTimeSolarisGraph;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class CpuSolarisMunins extends MuninsProbe {
	static private final Logger logger = JrdsLogger.getLogger(CpuSolarisMunins.class.getPackage().getName());

	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("ssCpuRawUser", ProbeDesc.COUNTER, "user.value");
		pd.add("ssCpuRawWait", ProbeDesc.COUNTER, "waitio.value");
		pd.add("ssCpuRawKernel", ProbeDesc.COUNTER, "system.value");
		pd.add("ssCpuRawIdle", ProbeDesc.COUNTER, "idle.value");

		pd.setRrdName("cpusolarismunins");
		pd.setMuninsProbesNames(new String[] { "cpu"});
		pd.setGraphClasses(new Class[] {CpuRawTimeSolarisGraph.class});
	}
	
	/**
	 * @param monitoredHost
	 */
	public CpuSolarisMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
