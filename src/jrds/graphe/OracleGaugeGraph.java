/*
 * Created on 8 mars 2005
 *
 * TODO 
 */
package jrds.graphe;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;

/**
 * @author bacchell
 *
 * TODO 
 */
public class OracleGaugeGraph extends RdsGraph {
	
	static final GraphDesc gd = new GraphDesc(2);
	static {
		gd.add("logonscurr", GraphDesc.LINE, "logons current");
		gd.add("opcurscurr", GraphDesc.LINE, "opened cursors current");
		gd.setVerticalLabel("value");
		gd.setHostTree(GraphDesc.HSDJT);
		gd.setViewTree(GraphDesc.SDJT);
	}

	/**
	 * @param theStore
	 */
	public OracleGaugeGraph(Probe theStore) {
		super(theStore, gd);
		setGraphName(theStore.getName());
		setGraphTitle("Ouverture sur la base " + ((jrds.probe.Oracle) probe).getSid());
	}

}
