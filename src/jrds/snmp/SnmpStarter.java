package jrds.snmp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import jrds.probe.snmp.SnmpProbe;
import jrds.starter.Resolver;
import jrds.starter.Starter;
import jrds.starter.StarterNode;

import org.apache.log4j.Level;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;

public class SnmpStarter extends Starter {
	static final String TCP = "tcp";
	static final String UDP = "udp";
	static final private OID hrSystemUptime = new OID(".1.3.6.1.2.1.25.1.1.0");
	static final private OID sysUpTimeInstance = new OID(".1.3.6.1.2.1.1.3.0");

	static final private PDUFactory pdufactory = new DefaultPDUFactory(PDU.GET);

	private int version = SnmpConstants.version2c;
	private String proto = UDP;
	private String hostname = null;
	private int port = 161;
	private String community = "public";
	//A default value for the uptime OID, from the HOST-RESSOURCES MIB
	private OID uptimeOid = hrSystemUptime;
	private Target snmpTarget;

	public SnmpStarter() {
	}

	public SnmpStarter(String community, String version) {
		super();
		setVersion(version);
		this.community = community;
	}

	public SnmpStarter(String community, String version, String portStr) {
		super();
		setVersion(version);
		this.community = community;
		this.port = jrds.Util.parseStringNumber(portStr, 161);
	}

	@Override
	public boolean start() {
		boolean started = false;
		Resolver resolver = getLevel().find(Resolver.class);

		if(getLevel().find(MainStarter.class).isStarted() && resolver.isStarted()) {
			snmpTarget = makeTarget();
			if(snmpTarget != null) {
				started = readUpTime();
			}
		}
		return started;
	}

	private boolean readUpTime() {
		Set<OID> upTimesOids = new HashSet<OID>(2);
		upTimesOids.add(uptimeOid);
		//Fallback uptime OID, it should be always defined, from SNMPv2-MIB
		upTimesOids.add(sysUpTimeInstance);

		try {
			for(OID uptimeoid: upTimesOids) {
				PDU requestPDU = DefaultPDUFactory.createPDU(snmpTarget, PDU.GET);
				requestPDU.addOID(new VariableBinding(uptimeoid));
				Snmp snmp = getSnmp();
				ResponseEvent re = snmp.send(requestPDU, snmpTarget);
				if(re == null)
					throw new IOException("SNMP Timeout");
				PDU response = re.getResponse();
				if(response == null || re.getError() != null ) {
					Exception snmpException = re.getError();
					if(snmpException == null)
						snmpException = new IOException("SNMP Timeout");
					throw snmpException;
				}
				Object value = new SnmpVars(response).get(uptimeoid);
				if(value instanceof Number) {
					setUptime(((Number) value).longValue());
					return true;
				}
			}
		} catch (Exception e) {
			log(Level.ERROR, e, "Unable to get uptime: %s", e);
		}
		return false;
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
		Resolver resolver = getLevel().find(Resolver.class);

		if(UDP.equals(proto.toLowerCase())) {
			address = new UdpAddress(resolver.getInetAddress(), port);
		}
		else if(TCP.equals(proto.toLowerCase())) {
			address = new TcpAddress(resolver.getInetAddress(), port);
		}
		else {
			String addrStr = proto + ":" + hostname + "/" + port;
			address= GenericAddress.parse(addrStr);
			if(address == null) {
				log(Level.WARN, "Address " + addrStr + " not solvable");
			}
		}
		if(community != null && address != null) {
			retValue = new CommunityTarget(address, new OctetString(community));
			retValue.setVersion(version);
			retValue.setTimeout(getLevel().getHostList().getTimeout() * 1000 / 2);
			retValue.setRetries(1);
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
			log(Level.WARN, "version %s not valid", versionStr);
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
		if(port != null && ! "".equals(port))
			this.port = Integer.parseInt(port);
	}

	/**
	 * @param proto The proto to set.
	 */
	public void setProto(String proto) {
		this.proto = proto;
	}

	@Override
	public void initialize(StarterNode parent) {
		super.initialize(parent);
		if(parent instanceof SnmpProbe) {
			OID newUptimeOid = ((SnmpProbe)parent).getUptimeoid();
			if(newUptimeOid != null)
				uptimeOid = newUptimeOid;
		}
	}

	public Target getTarget() {
		if(snmpTarget == null)
			snmpTarget = makeTarget();
		return snmpTarget;
	}

	public Snmp getSnmp() {
		Snmp retValue = null;
		retValue = getLevel().find(MainStarter.class).snmp;
		return retValue;
	}

	@Override
	public String toString() {
		return "snmp:" + proto + "://" + this.hostname + ":" + port;
	}

	public void setUptimeOid(OID uptimeOid) {
		this.uptimeOid = uptimeOid;
	}

	/**
	 * @return the pdufactory
	 */
	public PDUFactory getPdufactory() {
		return pdufactory;
	}

	/* (non-Javadoc)
	 * @see jrds.starter.Starter#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return super.isStarted() && getLevel().find(MainStarter.class).isStarted()  && getLevel().find(Resolver.class).isStarted();
	}
}
