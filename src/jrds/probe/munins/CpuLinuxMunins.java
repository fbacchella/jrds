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
import jrds.graphe.CpuLinuxMuninsGraph;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class CpuLinuxMunins extends MuninsProbe {
	static private final Logger logger = JrdsLogger.getLogger(CpuLinuxMunins.class.getPackage().getName());

	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("user", ProbeDesc.COUNTER, "user.value");
		pd.add("nice", ProbeDesc.COUNTER, "nice.value");
		pd.add("system", ProbeDesc.COUNTER, "system.value");
		pd.add("idle", ProbeDesc.COUNTER, "idle.value");
		pd.add("iowait", ProbeDesc.COUNTER, "iowait.value");
		pd.add("irq", ProbeDesc.COUNTER, "irq.value");
		pd.add("softirq", ProbeDesc.COUNTER, "softirq.value");
		pd.setRrdName("cpulinuxmunins");
		pd.setMuninsProbesNames(new String[] { "cpu"});
		pd.setGraphClasses(new Class[] {CpuLinuxMuninsGraph.class});
	}
	
	/**
	 * @param monitoredHost
	 */
	public CpuLinuxMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
