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
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.SERVICES, GraphDesc.DATABASE, GraphDesc.JDBC, "DB Open"});
		gd.setViewTree(new Object[] {
				GraphDesc.SERVICES, GraphDesc.DATABASE, GraphDesc.JDBC, "DB Open"});
		gd.setGraphTitle("DB Open on {3}");
	}

	/**
	 * @param theStore
	 */
	public OracleGaugeGraph(Probe theStore) {
		super(theStore, gd);
		setGraphName(theStore.getName());
	}

}
