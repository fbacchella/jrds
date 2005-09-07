//----------------------------------------------------------------------------
//$Id$

package jrds.graphe;

import java.awt.Color;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.RdsGraph;

/**
 * The Graph associated with the average packet size
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class IfPacketSize
extends RdsGraph {
	static final GraphDesc ds = new GraphDesc(6);
	static {
		ds.add("ifOutUcastPkts");
		ds.add("ifOutOctets");
		ds.add("outPktSize", "ifOutOctets, ifOutUcastPkts,/", GraphDesc.AREA,
				Color.GREEN, "average packets size send");
		
		ds.add("ifInUcastPkts");
		ds.add("ifInOctets");
		ds.add("inPktSize", "0, ifInOctets, ifInUcastPkts,/,-", GraphDesc.AREA,
				Color.BLUE, "average packets size received");
		
		ds.setLowerLimit(Double.NaN);
		ds.setVerticalLabel("bytes");
		ds.setHostTree(new Object[] {
				GraphDesc.HOST, GraphDesc.NETWORK, GraphDesc.INTERFACES, GraphDesc.INDEX, "Packets size"});
		ds.setViewTree(new Object[] {
				GraphDesc.NETWORK, GraphDesc.INTERFACES, GraphDesc.HOST, GraphDesc.INDEX, "Packets size"});
		ds.setGraphName("ifpktssz-{2}");
		ds.setGraphTitle("Packets size on interface {2} on {1} ");
	}
	
	/**
	 * @param theStore
	 */
	public IfPacketSize(Probe theStore) {
		super(theStore, ds);
	}
}
