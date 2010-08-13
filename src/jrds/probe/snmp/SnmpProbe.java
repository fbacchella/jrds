package jrds.probe.snmp;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.snmp.SnmpRequester;
import jrds.snmp.SnmpStarter;

import org.apache.log4j.Level;
import org.snmp4j.smi.OID;

/**
 * A abstract class from which all snmp probes should be derived.<p>
 * An usefull command to browse the content of an snmp agent :<p>
 * <quote>snmpbulkwalk -OX -c public -v 2c hostname  enterprises | sed -e 's/\[.*\]//' -e 's/ =.*$//'|  grep '::' | uniq </quote>
 * @author bacchell
 */
public abstract class SnmpProbe extends Probe<OID, Object> {
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
			log(Level.ERROR, "No snmp connection configured");
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
				log(Level.TRACE, "Setting requester to %s", requesterName);
				requester = (SnmpRequester) SnmpRequester.class.getField(requesterName.toUpperCase()).get(null);
				readOK = true;
			}
			else {
				log(Level.ERROR, "No requester found");
			}
			if(uptimeOidName != null) {
				log(Level.TRACE, "Setting uptime OID to %s", uptimeOidName);
				uptimeoid = new OID(uptimeOidName);
				if(uptimeoid == null)
					readOK = false;
			}
		} catch (Exception e) {
			log(Level.ERROR, e, "Unable to read specific: %s", e.getMessage());
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
		Map<OID, Object> retValue = Collections.emptyMap();
		Collection<OID> oids = getOidSet();
		if(oids != null && getSnmpStarter().isStarted()) {
			try {
				Map<OID, Object> rawValues = requester.doSnmpGet(getSnmpStarter(), oids);
				retValue = new HashMap<OID, Object>(rawValues.size());
				for(Map.Entry<OID, Object> e: rawValues.entrySet()) {
					OID oid = new OID(e.getKey());
					oid.trim(getSuffixLength());
					retValue.put(oid, e.getValue());
				}
			} catch (IOException e) {
				log(Level.ERROR, e, "IO Error: %s", e.getMessage());
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
		return find(SnmpStarter.class);
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
