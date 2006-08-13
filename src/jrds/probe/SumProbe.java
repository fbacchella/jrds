/*##########################################################################
_##
_##  $Id: BackEndCommiter.java 235 2006-03-01 21:29:48 +0100 (mer., 01 mars 2006) fbacchella $
_##
_##########################################################################*/

package jrds.probe;

import java.util.Collection;
import java.util.Date;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.Sum;

public class SumProbe extends VirtualProbe {
	static final ProbeDesc pd = new ProbeDesc(0);
	static {
		pd.setGraphClasses(new Object[] {Sum.class});
	}
	private String graphName;
	Collection<String> graphList;

	public SumProbe(RdsHost thehost, String name, Collection<String> graphList) {
		super(thehost, pd);
		this.setName(name);
		this.graphList = graphList;
	}

	/**
	 * @return Returns the probeList.
	 */
	public Collection<String> getProbeList() {
		return graphList;
	}

	/**
	 * @return Returns the probeName.
	 */
	public String getGraphName() {
		return graphName;
	}

	public Date getLastUpdate() {
		return new Date();
	}
	@Override
	public String getSourceType() {
		return "virtual";
	}
}
