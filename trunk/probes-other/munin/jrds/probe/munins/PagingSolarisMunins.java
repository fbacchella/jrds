/*
 * Created on 7 janv. 2005
 *
 * TODO 
 */
package jrds.probe.munins;

import org.rrd4j.DsType;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.PagingSolarisMuninsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class PagingSolarisMunins extends MuninsProbe {
	static final private ProbeDesc pd = new ProbeDesc(1);
	static {
		pd.add("pgin", DsType.COUNTER, "pgin.value");
		pd.add("reclaim", DsType.COUNTER, "reclaim.value");
		pd.add("pgpgin", DsType.COUNTER, "pgpgin.value");
		pd.add("pgout", DsType.COUNTER, "pgout.value");
		pd.add("scan", DsType.COUNTER, "scan.value");
		pd.add("pgpgout", DsType.COUNTER, "pgpgout.value");
		pd.add("pgfree", DsType.COUNTER, "pgfree.value");
		pd.setGraphClasses(new Class[] {PagingSolarisMuninsGraph.class});
		pd.setMuninsProbesNames(new String[] { "paging_in", "paging_out" });
		pd.setProbeName("pageingmunins");
	}

	/**
	 * @param monitoredHost
	 */
	public PagingSolarisMunins(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}
}
