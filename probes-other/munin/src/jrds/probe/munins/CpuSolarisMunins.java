/*
 * Created on 11 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class CpuSolarisMunins extends MuninsProbe {
	static private final Logger logger = Logger.getLogger(CpuSolarisMunins.class);

	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("ssCpuRawUser", DsType.COUNTER, "user.value");
		pd.add("ssCpuRawWait", DsType.COUNTER, "waitio.value");
		pd.add("ssCpuRawKernel", DsType.COUNTER, "system.value");
		pd.add("ssCpuRawIdle", DsType.COUNTER, "idle.value");

		pd.setProbeName("cpusolarismunins");
		pd.setMuninsProbesNames(new String[] { "cpu"});
		pd.setGraphClasses(new String[] {"CpuRawTimeSolarisGraph"});
	}
	
	/**
	 * @param monitoredHost
	 */
	public CpuSolarisMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
