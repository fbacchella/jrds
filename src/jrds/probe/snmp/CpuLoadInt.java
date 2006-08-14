/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.Map;

/**
 * Used to get the load average, using the int value
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class CpuLoadInt
extends RdsSnmpSimple {
	public Map<?, Number> filterValues(Map snmpVars) {
		for(Map.Entry<?, Number> e: ((Map<?, Number>) snmpVars).entrySet()) {
			Number value = (Number) e.getValue();
			e.setValue(new Double(value.intValue()/100.0));
		}
		
		return (Map<?, Number>) snmpVars;
		
	}
}
