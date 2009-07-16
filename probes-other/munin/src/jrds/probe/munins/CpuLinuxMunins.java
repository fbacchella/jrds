/*
 * Created on 11 janv. 2005
 *
 * TODO
 */
package jrds.probe.munins;

import org.rrd4j.DsType;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.RdsHost;


/**
 * @author bacchell
 *
 * TODO
 */
public class CpuLinuxMunins extends MuninsProbe {
	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("user", DsType.COUNTER, "user.value");
		pd.add("nice", DsType.COUNTER, "nice.value");
		pd.add("system", DsType.COUNTER, "system.value");
		pd.add("idle", DsType.COUNTER, "idle.value");
		pd.add("iowait", DsType.COUNTER, "iowait.value");
		pd.add("irq", DsType.COUNTER, "irq.value");
		pd.add("softirq", DsType.COUNTER, "softirq.value");
		pd.setProbeName("cpulinuxmunins");
		pd.setMuninsProbesNames(new String[] { "cpu"});
		
		GraphDesc gd = new GraphDesc(7);
		gd.add("system", GraphDesc.AREA, "system");
		gd.add("user", GraphDesc.STACK, "user");
		gd.add("nice", GraphDesc.STACK, "nice");
		gd.add("idle", GraphDesc.STACK, "idle");
		gd.add("iowait", GraphDesc.STACK, "iowait");
		gd.add("irq", GraphDesc.STACK, "irq");
		gd.add("softirq", GraphDesc.STACK, "softirq");
		gd.setGraphName("cpulinux");
		gd.setGraphName("CPU usage");
		gd.setVerticalLabel("%");
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.SYSTEM, GraphDesc.LOAD, "CPU usage"});
		gd.setViewTree(new Object[] {
				GraphDesc.SYSTEM, GraphDesc.LOAD, "CPU usage", GraphDesc.HOST});
		
		pd.setGraphClasses(new Object[] {gd});
	}
	
	/**
	 * @param monitoredHost
	 */
	public CpuLinuxMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
