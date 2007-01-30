/*
 * Created on 23 nov. 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds.probe.snmp;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.snmp.SnmpRequester;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class SnmpProbe extends Probe {
	static final private Logger logger = Logger.getLogger(SnmpProbe.class);

	private Map<OID, String> nameMap = null;
	private SnmpRequester requester;

	public SnmpProbe(RdsHost monitoredHost, ProbeDesc pd)
	{
		super(monitoredHost, pd);
		if(nameMap == null)
			nameMap = getPd().getCollectOids();
		requester = getSnmpRequester();
	}

	public SnmpProbe()
	{
		super();
	}

	public void setPd(ProbeDesc pd) {
		super.setPd(pd);
		if(nameMap == null)
			nameMap = getPd().getCollectOids();
		requester = getSnmpRequester();
		
	}

	protected SnmpRequester getSnmpRequester() {
		return getPd().getRequester();
  	}

	private Map<OID, String> initNameMap()
	{
		return getPd().getCollectOids();
	}

	public Map<OID, String> getOidNameMap()
	{
		if(nameMap == null)
			nameMap = initNameMap();
		return nameMap;
	}

	protected abstract Set<OID> getOidSet();


	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map retValue = null;
		if(getSnmpStarter().isStarted()) {
			Collection<OID> oids = getOidSet();
			if(oids != null) {
				try {
					retValue = requester.doSnmpGet(this.getSnmpStarter(), oids);
				} catch (IOException e) {
					logger.error("SNMP error with probe " + this + ": " +e);
				}
			}
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
	@SuppressWarnings("unchecked")
	public Map<?, Number> filterValues(Map snmpVars) {
		Map<OID, Number> retValue = new HashMap<OID, Number>(snmpVars.size());
		for(Map.Entry<OID, Object> e: ((Map<OID, Object> )snmpVars).entrySet()) {
			OID oid = e.getKey();
			oid.removeLast();
			Object o = e.getValue();
			if( o instanceof Number) {
				retValue.put(oid, (Number)o);
			}
			if( o instanceof Date) {
				Date value = (Date) o;
				retValue.put(oid, new Double(value.getTime()));
			}
			
		}
		return retValue;
	}

	public SnmpStarter getSnmpStarter() {
		return (SnmpStarter) getStarters().find(SnmpStarter.SNMPKEY);
	}

	@Override
	public String getSourceType() {
		return "SNMP";
	}

	@Override
	public boolean isStarted() {
		return super.isStarted() && getSnmpStarter().isStarted();
	}

	@Override
	public long getUptime() {
		return getSnmpStarter().getUptime();
	}

	
}
