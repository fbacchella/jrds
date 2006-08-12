/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.Iterator;
import java.util.Map;

/**
 * Used to get the load average, using the int value
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class CpuLoadInt
extends RdsSnmpSimple {
	public Map filterValues(Map snmpVars) {
		for(Iterator i = snmpVars.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			Number value = (Number) e.getValue();
			e.setValue(new Double(value.intValue()/100.0));
		}
		
		return snmpVars;
		
	}
}
