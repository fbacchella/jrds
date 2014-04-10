/*
 * Created on 8 mars 2005
 *
 * TODO 
 */
package jrds.probe.jdbc;

import java.util.Arrays;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.GraphNode;
import jrds.PropertiesManager;

/**
 * @author bacchell
 *
 * TODO 
 */
public class OracleActivityGraph extends GraphNode {

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
        gd.setTree(PropertiesManager.HOSTSTAB, Arrays.asList(new Object[] {
                GraphDesc.HOST, GraphDesc.SERVICES, GraphDesc.DATABASE, GraphDesc.JDBC, "DB activity"}));
        gd.setTree(PropertiesManager.VIEWSTAB, Arrays.asList(new Object[] {
                GraphDesc.SERVICES, GraphDesc.DATABASE, GraphDesc.JDBC, "DB activity"}));
        gd.setGraphTitle("DB activity on {3}");
        gd.setGraphName("{4}");
    }

    /**
     * @param theStore
     */
    public OracleActivityGraph(Probe<?,?> theStore) {
        super(theStore, gd);
    }

}
