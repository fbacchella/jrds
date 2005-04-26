/*
 * Created on 23 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import java.awt.Color;
import java.util.Map;

import jrds.GraphDesc;
import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;

/**
 * Used to get the load average, using the float value
 * @author Fabrice Bacchella
 */
public class CpuLoad
    extends RdsSnmpSimple {
    static final private Logger logger = JrdsLogger.getLogger(CpuLoad.class.
        getPackage().getName());

    static final private ProbeDesc pd = new ProbeDesc(3);
    static {
        pd.add("la1", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.1"));
        pd.add("la5", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.2"));
        pd.add("la15", ProbeDesc.GAUGE, new OID(".1.3.6.1.4.1.2021.10.1.6.3"));
        pd.setRrdName("laverage");
        pd.setRequester(SnmpRequester.RAW);

        GraphDesc gd = new GraphDesc(3);

        gd.setFilename("LoadAverage");
        gd.setGraphTitle("Charge CPU");
        gd.add("la1", GraphDesc.LINE, Color.GREEN, "1mn");
        gd.add("la5", GraphDesc.LINE, Color.BLUE, "5mn");
        gd.add("la15", GraphDesc.LINE, Color.RED, "15mn");
        gd.setVerticalLabel("queue size");
        gd.setHostTree(GraphDesc.HSLT);
        gd.setViewTree(GraphDesc.SLHT);

        pd.setGraphClasses(new Object[] {gd});
    }

    /**
     * simple constructor
     *
     * @param monitoredHost RdsHost
     */
    public CpuLoad(RdsHost monitoredHost) {
        super(monitoredHost, pd);
    }

    /**
     * No need to play with the OID, so the snmpVars map can be returned has is
     *
     * @param snmpVars Map
     * @return unmodified map
     */
    public Map filterValues(Map snmpVars) {
        return snmpVars;
    }
}
