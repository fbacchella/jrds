/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import java.util.Set;

import org.snmp4j.smi.OID;

import jrds.ProbeDesc;
import jrds.RdsHost;


/**
 * This probe is used to do simple mapping from oid to datastore
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class RdsSnmpSimple extends SnmpProbe  {
	public RdsSnmpSimple(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
	}
	public RdsSnmpSimple() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.snmp.SnmpPreparator#makeOidSet()
	 */
	public Set<OID> getOidSet() {
		return getOidNameMap().keySet();
	}
}
