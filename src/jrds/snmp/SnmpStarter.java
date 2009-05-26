package jrds.snmp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jrds.Starter;
import jrds.StarterNode;
import jrds.StartersSet;
import jrds.probe.snmp.SnmpProbe;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpStarter extends Starter {
	static final private Logger logger = Logger.getLogger(SnmpStarter.class);
	static final String TCP = "tcp";
	static final String UDP = "udp";
	static public final String SNMPKEY = "snmp";
	static final private OID hrSystemUptime = new OID(".1.3.6.1.2.1.25.1.1.0");
	static final private OID sysUpTimeInstance = new OID(".1.3.6.1.2.1.1.3.0");

	static private Snmp snmp = null;
	static public final Starter full = new Starter() {
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

	private int version = SnmpConstants.version2c;
	private String proto = UDP;
	private String hostname = null;
	private int port = 161;
	private String community = "public";
	private Starter.Resolver resolver = null;
	//A default value for the uptime OID, from the HOST-RESSOURCES MIB
	private OID uptimeOid = hrSystemUptime;
	private Target snmpTarget;

	/* (non-Javadoc)
	 * @see jrds.Starter#register(jrds.StarterNode)
	 */
	@Override
	public Starter register(StarterNode node) {
		if(node instanceof SnmpProbe) {
			OID newUptimeOid = ((SnmpProbe)node).getUptimeoid();
			if(newUptimeOid != null)
				uptimeOid = newUptimeOid;
		}
		return super.register(node);
	}

	@Override
	public boolean start() {
		boolean started = false;
		if(full.isStarted() && resolver.isStarted()) {
			snmpTarget = makeTarget();
			if(snmpTarget != null) {

				try {
					Set<OID> upTimesOids = new HashSet<OID>(2);
					upTimesOids.add(uptimeOid);
					//Fallback uptime OID, it should be always defined, from SNMPv2-MIB
					upTimesOids.add(sysUpTimeInstance);

					Map<OID, Object> uptimes = SnmpRequester.RAW.doSnmpGet(this, upTimesOids);
					Number uptimeNumber = (Number) uptimes.get(uptimeOid);
					//Try the fallback
					if(uptimeNumber == null)
						uptimeNumber = (Number) uptimes.get(sysUpTimeInstance);
					if(uptimeNumber != null) {
						setUptime(uptimeNumber.longValue());
						started = true;
					}
				} catch (IOException e) {
					logger.error("Unable to get uptime for " + snmpTarget + " because of: " + e);
				}
			}
		}
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
		Address address;
		if(UDP.equals(proto.toLowerCase())) {
			address = new UdpAddress(resolver.getInetAddress(), port);
		}
		else {
			String addrStr = proto + ":" + this.hostname + "/" + port;
			address= GenericAddress.parse(addrStr);
			if(address == null) {
				logger.warn("Address " + addrStr + " not solvable");
			}
		}
		if(community != null && address != null) {
			retValue = new CommunityTarget(address, new OctetString(community));
			retValue.setVersion(version);
			retValue.setTimeout(retValue.getTimeout() * 5);
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
	public void initialize(StarterNode parent, StartersSet level) {
		super.initialize(parent, level);
		level.find(Starter.Resolver.buildKey(hostname));
		resolver = (Resolver) level.find(Starter.Resolver.buildKey(hostname));
		if( level.find(full.getKey()) == null) {
			level.getRoot().registerStarter(full, level.getRoot().getLevel());
		}
	}

	public Target getTarget() {
		if(snmpTarget == null)
			snmpTarget = makeTarget();
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
		return "snmp:" + proto + "://" + this.hostname + ":" + port;
	}

	public void setUptimeOid(OID uptimeOid) {
		this.uptimeOid = uptimeOid;
	}
}
