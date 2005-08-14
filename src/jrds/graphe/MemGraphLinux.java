/*
 * Created on 29 déc. 2004
 *
 * TODO 
 */
package jrds.graphe;

import java.awt.Color;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class MemGraphLinux extends RdsGraph {
	static final GraphDesc ds = new GraphDesc(16);
	static {
		ds.add("memTotalSwap");
		ds.add("memAvailSwap");
		ds.add("memTotalReal");
		ds.add("memAvailReal");
		ds.add("memShared");
		ds.add("memBuffer");
		ds.add("memCached");

		ds.add("memUsedReal", "memTotalReal, memAvailReal, -");
		ds.add("memActiveReal", "memUsedReal, memBuffer, -, memCached, -");
		ds.add("memUsedSwap", "memTotalSwap, memAvailSwap, -");
		
		ds.add("memSharedBytes", "memShared, 1024, *", GraphDesc.LINE, Color.RED,"Total Shared Memory");

		ds.add("memActiveRealBytes", "memActiveReal, 1024, *", GraphDesc.AREA, Color.GREEN, "Active Real/Physical Memory Space");
		ds.add("memCachedBytes", "memCached, 1024, *", GraphDesc.STACK, Color.MAGENTA, "Total Cached Memory");
		ds.add("memBufferBytes", "memBuffer, 1024, *", GraphDesc.STACK, Color.ORANGE,"Total Buffered Memory");
		ds.add("memAvailRealBytes", "memAvailReal, 1024, *",GraphDesc.STACK, Color.BLACK,"Available Real/Physical Memory Space");
		ds.add("memUsedSwapBytes", "memUsedSwap, 1024, *", GraphDesc.STACK,Color.RED,"Used Swap Space");
		ds.setFilename("memory");
		ds.setGraphName("Utilisation mémoire");
		ds.setHostTree(GraphDesc.HSMT);
		ds.setViewTree(GraphDesc.SMHT);

	}


	/**
	 * @param theStore
	 */
	public MemGraphLinux(Probe theStore) {
		super(theStore, ds);
	}
}
