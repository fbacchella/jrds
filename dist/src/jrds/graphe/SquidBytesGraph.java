/*
 * Created on 25 janv. 2005
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
public class SquidBytesGraph extends RdsGraph {
	
	static final GraphDesc ds = new GraphDesc(2);
	static {
		ds.add("HttpOutKb");
		ds.add("HttpOut", "HttpOutKb, 1024, *", GraphDesc.LINE, "Bytes send to client");
		ds.add("HttpInKb");
		ds.add("HttpIn", "HttpInKb, 1024, *", GraphDesc.LINE, "Bytes received from client");
		ds.add("ServerOutKb");
		ds.add("ServerOut", "ServerOutKb, 1024, *", GraphDesc.LINE, "Bytes send to servers");
		ds.add("ServerInKb");
		ds.add("ServerIn", "ServerInKb, 1024, *", GraphDesc.LINE, "Bytes received from servers");


		ds.setFilename("squidbytes");
		ds.setGraphTitle("Squid bytes transfered");
		ds.setVerticalLabel("Bytes/s");
		ds.setHostTree(new Object[] {GraphDesc.HOST, GraphDesc.SERVICES, "Squid", GraphDesc.TITLE} );
		ds.setViewTree(new Object[] {GraphDesc.SERVICES, "Squid", GraphDesc.HOST, GraphDesc.TITLE});
	}
	
	/**
	 * @param theStore
	 */
	public SquidBytesGraph(Probe theStore) {
		super(theStore, ds);
	}
}
