package jrds.snmp;

import java.io.IOException;

import jrds.Starter;
import jrds.StartersSet;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpStarter extends Starter {
	static private Logger logger = Logger.getLogger(SnmpStarter.class);
	static final String TCP = "tcp";
	static final String UDP = "udp";
	static public final String SNMPKEY = "snmp";

	static private Snmp snmp = null;
	static final Starter full = new Starter() {
		public boolean start() {
			boolean started = false;
			try {
				snmp = new Snmp(new DefaultUdpTransportMapping());
				snmp.listen();
				started = true;
			} catch (IOException e) {
				logger.error("Snmp activity not started : " +  e);
				snmp = null;
			}
			return started;
		}
		public void stop () {
			try {
				snmp.close();
			} catch (IOException e) {
			}
			snmp = null;
		}
		@Override
		public Object getKey() {
			return full.getClass();
		}
		@Override
		public String toString() {
			return "SNMP root";
		}
	};

	private int version = SnmpConstants.version1;
	private String proto = UDP;
	private String hostname = null;
	private int port = 161;
	private String community = "public";

	private Target snmpTarget;
	
	@Override
	public boolean start() {
		boolean started = false;
		if(full.isStarted()) {
			snmpTarget = makeTarget();
			started = snmpTarget != null;
		}
		else
			logger.debug("root SNMP not started");
		return started;
	}

	/**
	 * Make the target filled with the predefined values
	 * return null if the adress is invalid
	 * @return the target just build or null in case of error
	 */
	private Target makeTarget()
	{
		Target retValue = null;
		String addrStr = proto + ":" + this.hostname + "/" + port;
		try {
			Address address = GenericAddress.parse(addrStr);
			if(address == null) {
				logger.warn("Address " + addrStr + " not solvable");
			}
			if(community != null && address != null) {
				retValue = new CommunityTarget(address, new OctetString(community));
				retValue.setVersion(version);
				retValue.setTimeout(retValue.getTimeout() * 5);
			}
		}
		catch(IllegalArgumentException ex) {
			logger.warn("Adresse definition incorrect: " + addrStr +": " + ex);
			retValue = null;
		}
		return retValue;
	}


	@Override
	public void stop() {
		snmpTarget = null;
	}

	public void setVersion(String versionStr)
	{
		if("1".equals(versionStr)) {
			version = SnmpConstants.version1;
		}
		else if("2".equals(versionStr) || "2c".equals(versionStr)) {
			version = SnmpConstants.version2c;
		}
		else if("3".equals(versionStr)) {
			version = SnmpConstants.version3;
		}
		else
			logger.warn("version " + versionStr + " not valid");
	}

	public void setVersion(int version)
	{
		this.version = version;
	}

	/**
	 * @param community The community to set.
	 */
	public void setCommunity(String community) {
		this.community = community;
	}
	/**
	 * @param hostname The hostname to set.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(String port) {
		if(port != null)
			this.port = Integer.parseInt(port);
	}
	/**
	 * @param proto The proto to set.
	 */
	public void setProto(String proto) {
		this.proto = proto;
	}
	@Override
	public void initialize(Object parent, StartersSet level) {
		super.initialize(parent, level);
		if( level.find(full.getKey()) == null)
			level.getRoot().registerStarter(full, level.getRoot().getLevel());
			//HostsList.getRootGroup().getStarters().registerStarter(full, HostsList.getRootGroup());
	}

	public Target getTarget() {
		return snmpTarget;
	}

	public Snmp getSnmp() {
		Snmp retValue = null;
		if(full.isStarted())
			retValue = snmp;
		return retValue;
	}
	@Override
	public Object getKey() {
		return SNMPKEY;

	}

	@Override
	public String toString() {
		return proto + ":" + this.hostname + "/" + port;
	}


}
