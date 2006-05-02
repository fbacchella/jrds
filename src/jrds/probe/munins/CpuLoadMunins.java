/*
 * Created on 7 janv. 2005
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
public class CpuLoadMunins extends MuninsProbe {
	static private final Logger logger = Logger.getLogger(CpuLoadMunins.class);

	static final private ProbeDesc pd = new ProbeDesc(1);
	static {
		pd.add("la1", DsType.GAUGE, "load.value");
		pd.setMuninsProbesNames(new String[] { "load" });
		pd.setProbeName("laveragemunins");

		pd.setGraphClasses(new Object[] {"cpuload"});
	}

	/**
	 * @param monitoredHost
	 */
	public CpuLoadMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
