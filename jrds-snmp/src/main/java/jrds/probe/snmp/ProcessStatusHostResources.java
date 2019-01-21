/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.Arrays;
import java.util.Map;

import jrds.JrdsSample;

import org.snmp4j.smi.OID;

/**
 * @author Fabrice Bacchella
 */
public class ProcessStatusHostResources extends RdsSnmpSimple {
    
    private enum RUNSTAT {
        running,
        runnable,
        notRunnable,
        invalid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#modifySample(org.rrd4j.core.Sample, java.util.Map)
     */
    @Override
    public void modifySample(JrdsSample oneSample, Map<OID, Object> snmpVars) {
        int[] counts = new int[RUNSTAT.values().length];
        Arrays.fill(counts, 0);
        for(Object status: snmpVars.values()) {
            if(status == null)
                continue;
            if(!(status instanceof Number))
                continue;
            // ordinal is stat value - 1
            int state = ((Number) status).intValue();
            counts[state - 1]++;
        }
        snmpVars.clear();
        oneSample.clear();
        for (RUNSTAT stat: RUNSTAT.values()) {
            oneSample.put(stat.name(), counts[stat.ordinal()]);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.probe.snmp.SnmpProbe#getSuffixLength()
     */
    @Override
    public int getSuffixLength() {
        return 0;
    }

}
