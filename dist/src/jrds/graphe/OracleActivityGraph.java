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
public class OracleActivityGraph extends RdsGraph {
	
	static final GraphDesc gd = new GraphDesc(7);
	static {
		gd.add("logonscum", GraphDesc.LINE, "logon/s");
		gd.add("opcurcum", GraphDesc.LINE, "opened cursors/s");
		gd.add("usercommit", GraphDesc.LINE, "user commits/s");
		gd.add("userrollbacks", GraphDesc.LINE, "user rollbacks/s");
		gd.add("usercalls", GraphDesc.LINE, "user calls");
		gd.add("msgsent", GraphDesc.LINE, "messages sent/s");
		gd.add("msgrcvd", GraphDesc.LINE, "messages received/s");
		gd.setVerticalLabel("operation/s");
		gd.setHostTree(GraphDesc.HSDJT);
		gd.setViewTree(GraphDesc.SDJT);
	}

	/**
	 * @param theStore
	 */
	public OracleActivityGraph(Probe theStore) {
		super(theStore, gd);
		setFilename(theStore.getName());
		setGraphTitle("Activité sur la base " + ((jrds.probe.Oracle) probe).getSid());
	}

}
