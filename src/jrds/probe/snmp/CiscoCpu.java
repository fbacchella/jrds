package jrds.probe.snmp;

// ----------------------------------------------------------------------------
// $Id$

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;

import org.snmp4j.smi.OID;
import jrds.GraphDesc;
import java.awt.Color;


/**
 * A class to probe the CPU load on a cisco
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class CiscoCpu extends RdsSnmpSimple {
    static final private ProbeDesc pd = new ProbeDesc(2);
    static {
        pd.add("la1", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.57"));
        pd.add("la5", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.9.2.1.58"));
        pd.setRrdName("ciscocpuload");

        GraphDesc gd = new GraphDesc(2);
        gd.add("la1", GraphDesc.LINE, Color.GREEN, "1mn");
        gd.add("la5", GraphDesc.LINE, Color.BLUE, "5mn");
        gd.setFilename("ciscocpu");
        gd.setGraphTitle("Charge CPU");
        gd.setHostTree(GraphDesc.HSLT);
        gd.setViewTree(GraphDesc.SLHT);

        pd.setGraphClasses(new Object[] {gd});
    }

    /**
     * A basic, default constructor
     * @param monitoredHost
     */
    public CiscoCpu(RdsHost monitoredHost) {
        super(monitoredHost, pd);
    }

}
