/*
 * Created on 7 janv. 2005
 *
 * TODO
 */
package jrds.probe.munins;

import org.rrd4j.DsType;

import jrds.ProbeDesc;
import jrds.RdsHost;

/**
 * @author bacchell
 *
 * TODO
 */
public class CpuLoadMunins extends Munin {
	static final private ProbeDesc pd = new ProbeDesc(1);
	static {
		pd.add("la1", DsType.GAUGE, "load.value");
		//pd.setMuninsProbesNames(new String[] { "load" });
		pd.setProbeName("laveragemunins");

		//pd.setGraphClasses(new Object[] {"cpuload"});
	}
}
