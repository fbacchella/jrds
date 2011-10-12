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
public class IfSolarisMunins extends MuninIndexed {
	static final private ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("ifInOctets", DsType.COUNTER, "rbytes.value");
		pd.add("ifOutOctets", DsType.COUNTER, "obytes.value");
		pd.add("ifInErrors", DsType.COUNTER, "ierrors.value");
		pd.add("ifOutErrors", DsType.COUNTER, "oerrors.value");
		pd.add("collisions", DsType.COUNTER, "collisions.value");
//		pd.setGraphClasses(new Object[] {"ifbps"});
//		pd.setMuninsProbesNames(new String[] { "if_${index}", "if_errcoll_${index}"});
		pd.setProbeName("if-_${index}_munins");
	}
}
