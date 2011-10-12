/*
 * Created on 7 janv. 2005
 *
 * TODO
 */
package jrds.probe.munin;

import jrds.objects.probe.ProbeDesc;

import org.rrd4j.DsType;

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
