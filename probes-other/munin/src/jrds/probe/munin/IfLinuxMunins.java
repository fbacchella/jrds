package jrds.probe.munin;

import jrds.objects.probe.ProbeDesc;

import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class IfLinuxMunins extends MuninIndexed {

	static final private ProbeDesc pd = new ProbeDesc(4);
	static {
		pd.add("ifInOctets", DsType.COUNTER, "down.value");
		pd.add("ifOutOctets", DsType.COUNTER, "up.value");
		pd.add("ifInErrors", DsType.COUNTER, "rcvd.value");
		pd.add("ifOutErrors", DsType.COUNTER, "trans.value");
//		pd.setMuninsProbesNames(new String[] { "if_${index}", "if_err_${index}"});
//		pd.setGraphClasses(new Object[] {"ifbps"});
		pd.setProbeName("if-${index}_munins");
	}
}
