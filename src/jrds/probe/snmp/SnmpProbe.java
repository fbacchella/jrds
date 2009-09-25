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
public abstract class SnmpProbe extends Probe<OID, Object> {
	static final private Logger logger = Logger.getLogger(SnmpProbe.class);

	public final static String REQUESTERNAME = "requester";
	public final static String UPTIMEOIDNAME = "uptimeOid";
	private Map<OID, String> nameMap = null;
	private SnmpRequester requester;
	private int suffixLength = 1;
	private OID uptimeoid = null;

	/* (non-Javadoc)
	 * @see jrds.Probe#setPd(jrds.ProbeDesc)
	 */
	@Override
	public void setPd(ProbeDesc pd) {
		super.setPd(pd);
		nameMap = getPd().getCollectOids();
	}
	
	public boolean configure() {
		SnmpStarter snmp = getSnmpStarter();
		if(snmp == null) {
			logger.error("No snmp connection configured for " + this);
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#readSpecific()
	 */
	@Override
	public boolean readSpecific() {
		boolean readOK = false;
		String requesterName =  getPd().getSpecific(REQUESTERNAME);
		String uptimeOidName =  getPd().getSpecific(UPTIMEOIDNAME);
		try {
			if(requesterName != null) {
				logger.trace("Setting requester to " + requesterName);
				requester = (SnmpRequester) SnmpRequester.class.getField(requesterName.toUpperCase()).get(null);
				readOK = true;
			}
			else {
				logger.error("No requester found");
			}
			if(uptimeOidName != null) {
				logger.trace("Setting uptime OID to " + uptimeOidName);
				uptimeoid = new OID(uptimeOidName);
				if(uptimeoid == null)
					readOK = false;
			}
		} catch (Exception e) {
			logger.error("Unable to read specific: "+ e);
		}
		return readOK && super.readSpecific();
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
	@Override
	public Map<OID, Object> getNewSampleValues() {
		Map<OID, Object> retValue = null;
		if(getSnmpStarter().isStarted()) {
			Collection<OID> oids = getOidSet();
			if(oids != null) {
				try {
					retValue = requester.doSnmpGet(getSnmpStarter(), oids);
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
	@Override
	public Map<OID, Number> filterValues(Map<OID, Object>snmpVars) {
		Map<OID, Number> retValue = new HashMap<OID, Number>(snmpVars.size());
		for(Map.Entry<OID, Object> e: snmpVars.entrySet()) {
			OID oid = e.getKey();
			for(int i= 0; i < getSuffixLength(); i++)
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
	public boolean isCollectRunning() {
		return super.isCollectRunning() && getSnmpStarter().isStarted();
	}

	@Override
	public long getUptime() {
		return getSnmpStarter().getUptime();
	}

	public int getSuffixLength() {
		return suffixLength;
	}

	public void setSuffixLength(int suffixLength) {
		this.suffixLength = suffixLength;
	}

	/**
	 * @return the uptimeoid
	 */
	public OID getUptimeoid() {
		return uptimeoid;
	}
}
