/*
 * Created on 8 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.util.LinkedList;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class PagingSolarisMuninsGraph extends RdsGraph {
	static final GraphDesc gd = new GraphDesc(7);
	static {
		gd.setFilename("pagesolmunins");
		gd.setGraphTitle("Paging activity");
		gd.setVerticalLabel("operation/s");
		gd.add(GraphDesc.COMMENT,"Upward graph");
		gd.add("pgin",GraphDesc.LINE, "Page-in requests");
		gd.add("pgpgin",GraphDesc.LINE, "pages paged-in");
		gd.add("reclaim",GraphDesc.LINE, "Pages reclaimed");
		gd.colorsReset();
		gd.add(GraphDesc.COMMENT,"Downard graph");
		gd.add("pgout");
		gd.add("scan");
		gd.add("pgpgout");
		gd.add("pgfree");
		gd.add("rpgout","0, pgout, - ", GraphDesc.LINE, "page-out requests");
		gd.add("rpgpgout","0, scan, - ", GraphDesc.LINE, "pages paged-out");
		gd.add("rscan","0, scan, - ", GraphDesc.LINE, "pages per second scanned by");
		gd.add("rpgfree","0, pgfree, - ", GraphDesc.LINE, "pages per second placed on the free list");
	}
	/**
	 * @param theStore
	 */
	public PagingSolarisMuninsGraph(Probe theStore) {
		super(theStore, gd);
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByHost()
	 */
	public LinkedList getTreePathByHost() {
		LinkedList retValue = new LinkedList();
		retValue.add(this.probe.getHost().getName());
		retValue.add("Système");
		retValue.add("Mémoire");
		retValue.add(getGraphTitle());
		return retValue;
	}
	
	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsGraph#getTreePathByView()
	 */
	public LinkedList getTreePathByView() {
		LinkedList retValue = new LinkedList();
		retValue.add("Système");
		retValue.add("Mémoire");
		retValue.add(getGraphTitle());
		retValue.add(this.probe.getHost().getName());
		return retValue;
	}
}
