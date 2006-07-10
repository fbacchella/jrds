/*
 * Created on 24 janv. 2005
 *
 * TODO 
 */
package jrds.snmp;

import org.apache.log4j.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;



/**
 * @author bacchell
 *
 * TODO 
 */
public class TargetFactory {
	static final private Logger logger = Logger.getLogger(TargetFactory.class);
	static final TargetFactory instance=new TargetFactory();
	static final String TCP = "tcp";
	static final String UDP = "udp";
	
	int version;
	String community;
	String hostname;
	int port;
	String proto;
	Address address;
	String addrStr;
	Target target;
	

	/**
	 * A private constructor, it's a singloton factory
	 */
	private TargetFactory() {
		
	}
	
	public void prepareNew()
	{
		version = SnmpConstants.version1;
		community = "public";
		hostname="127.0.0.1";
		port=161;
		proto=UDP;
		addrStr=null;
	}
	
	public static TargetFactory getInstance()
	{
		return instance;
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
	
	/**
	 * Make the target filled with the predefined values
	 * return null if the adress is invalid
	 * @return the target just build or null in case of error
	 */
	public Target makeTarget()
	{
		Target retValue = null;
		String addrStr = proto + ":" + this.hostname + "/" + port;
		try {
			address = GenericAddress.parse(addrStr);
			if(address == null) {
				logger.warn("Address " + addrStr + " not solvable");
			}
			if(community != null && address != null)
				retValue = doCommunityTarget();
		}
		catch(IllegalArgumentException ex) {
			logger.warn("Adresse definition incorrect: " + addrStr +": " + ex.getLocalizedMessage());
			retValue = null;
		}
		return retValue;
	}
	
	private Target doCommunityTarget()
	{
		Target target = new CommunityTarget();
		((CommunityTarget)target).setCommunity(new OctetString(community));
		target.setAddress(address);
		target.setVersion(version);
		target.setTimeout(target.getTimeout() * 5);
		return target;
	}
}
