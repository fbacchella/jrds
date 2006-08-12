/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import java.util.Map;

/**
 * Used to just store some oid, with raw values
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class Raw
extends RdsSnmpSimple {	
	/**
	 * For this probe, raw result is used
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	public Map filterValues(Map snmpVars) {
		return snmpVars;
		
	}
}
