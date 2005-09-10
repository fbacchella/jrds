/*
 * Created on 7 janv. 2005
 *
 * TODO
 */
package jrds.probe.munins;

import jrds.JrdsLogger;
import jrds.MuninsProbe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 * TODO
 */
public class CpuLoadMunins extends MuninsProbe {
	static private final Logger logger = JrdsLogger.getLogger(CpuLoadMunins.class);

	static final private ProbeDesc pd = new ProbeDesc(1);
	static {
		pd.add("la1", ProbeDesc.GAUGE, "load.value");
		pd.setMuninsProbesNames(new String[] { "load" });
		pd.setRrdName("laveragemunins");

		pd.setGraphClasses(new Object[] {"cpuload.xml"});
	}

	/**
	 * @param monitoredHost
	 */
	public CpuLoadMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
