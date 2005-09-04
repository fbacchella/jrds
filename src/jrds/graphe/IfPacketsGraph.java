/*
 * Created on 7 févr. 2005
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
public class IfPacketsGraph
    extends RdsGraph {
    static final GraphDesc ds = new GraphDesc(6);
    static {
        ds.add(GraphDesc.COMMENT, "Upward graph");
        ds.add("ifOutUcastPkts", GraphDesc.AREA, Color.GREEN, "packets sends/s");
        ds.add("ifOutErrors", GraphDesc.LINE, Color.RED,
               "packets in error send/s");

        ds.add("ifInUcastPkts");
        ds.add("ifInErrors");
        ds.add(GraphDesc.COMMENT, "Downward graph");
        ds.add("ifInUcastInversed", "0, ifInUcastPkts,-", GraphDesc.AREA,
               Color.BLUE, "packets received/s");
        ds.add("ifInErrorsInversed", "0, ifInErrors,-", GraphDesc.LINE,
               Color.RED, "packets in error received/s");

        ds.setLowerLimit(Double.NaN);
        ds.setVerticalLabel("paquets/s");
        ds.setHostTree(GraphDesc.HNIIT);
        ds.setViewTree(GraphDesc.NIHIT);
        ds.setGraphName("ifpkts-{2}");
        ds.setGraphTitle("Packets exchanged on interface {2} on {1}");
    }

    /**
     * @param theStore
     */
    public IfPacketsGraph(Probe theStore) {
        super(theStore, ds);
    }
}
