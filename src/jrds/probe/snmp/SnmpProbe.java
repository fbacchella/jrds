/*
 * Created on 23 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;

import org.snmp4j.Target;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class SnmpProbe extends Probe {
	private Target snmpTarget;
	private Map nameMap = null;
	private SnmpRequester requester;

	public SnmpProbe(RdsHost monitoredHost, ProbeDesc pd)
	{
		super(monitoredHost, pd);
		if(nameMap == null)
			nameMap = getPd().getOidNameMap();
		requester = getSnmpRequester();
	}

	protected SnmpRequester getSnmpRequester() {
		return getPd().getRequester();
  	}

	private Map initNameMap()
	{
		return getPd().getOidNameMap();
	}

	public Map getOidNameMap()
	{
		if(nameMap == null)
			nameMap = initNameMap();
		return nameMap;
	}

	protected abstract Set getOidSet();


	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map retValue = null;
		Collection oids = getOidSet();
		if(oids != null) {
			retValue = requester.doSnmpGet(this, oids);
 		}
		return retValue;
	}

	/**
	 * Prepare the SnmpVars to be stored by a probe. In the general case, for a snmp probe
	 * the last element of the OID is removed.
	 * If the value is a date, the value is the second since epoch
	 * @param snmpVars
	 * @return a Map of all the identified vars
	 */
	public Map filterValues(Map snmpVars) {
		Map retValue = new HashMap (snmpVars.size());
		for(Iterator i = snmpVars.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			OID oid = (OID) e.getKey();
			oid.removeLast();
			Object o = e.getValue();
			if( o instanceof Number) {
				retValue.put(oid, o);
			}
			if( o instanceof Date) {
				Date value = (Date) o;
				retValue.put(oid, new Double(value.getTime()));
			}
			
		}
		return retValue;
	}
	/**
	 * @return Returns the snmpTarget.
	 */
	public Target getSnmpTarget() {
		Target retValue = null;
		if(snmpTarget != null)
			retValue = snmpTarget;
		else
			retValue = this.getHost().getTarget();
		return retValue;
	}
	/**
	 * @param snmpTarget The snmpTarget to set.
	 */
	public void setSnmpTarget(Target snmpTarget) {
		this.snmpTarget = snmpTarget;
	}
}
