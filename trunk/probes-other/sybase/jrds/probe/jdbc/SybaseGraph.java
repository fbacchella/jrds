/*
 * Created on 16 d�c. 2004
 *
 * TODO 
 */
package jrds.probe.jdbc;

import java.awt.Color;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.GraphNode;

/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseGraph extends GraphNode {
	static final private GraphDesc gd = new GraphDesc(5);
	static {
		gd.add("data", GraphDesc.AREA, Color.BLUE, "data");
		gd.add("index_size", GraphDesc.STACK, Color.GREEN, "index");
		gd.add("unused", GraphDesc.AREA, new Color(0,255,255), "unused");
		gd.add("database_size", GraphDesc.LINE, Color.RED, "total size");
		gd.add("reserved", GraphDesc.LINE, Color.BLACK, "reserved");
		gd.setVerticalLabel("bytes");
		gd.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.SERVICES, GraphDesc.DATABASE, GraphDesc.JDBC, "Disk usage"});
		gd.setViewTree(new Object[] {
				GraphDesc.SERVICES, GraphDesc.DATABASE, GraphDesc.JDBC, "Disk usage"});
		gd.setGraphTitle("Disk usage for {3}");
		gd.setGraphName("{4}");
	}

	/**
	 * @param theStore
	 */
	public SybaseGraph(Probe theStore) {
		super(theStore, gd);
	}
}
