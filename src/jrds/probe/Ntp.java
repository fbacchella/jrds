package jrds.probe;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.Probe;
import jrds.starter.Resolver;
import jrds.starter.SocketFactory;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Level;

public class Ntp extends Probe<String, Number> {
	static final NTPUDPClient client = new NTPUDPClient();
	int port = 123;

	public Boolean configure() {
		SocketFactory sf = (SocketFactory) getStarters().find(SocketFactory.makeKey(this)); 
		try {
			client.setSoTimeout(sf.getTimeout());
		} catch (SocketException e) {
			log(Level.ERROR, e, "Set timeout failed: %", e);
			return false;
		}
		return true;
	}
	
	public Boolean configure(Integer port) {
		this.port = port;
		return configure();
	}

	@Override
	public Map<String, Number> getNewSampleValues() {
		Resolver resolv = (Resolver) getStarters().find(Resolver.makeKey(this)); 
		try {
			TimeInfo ti = client.getTime(resolv.getInetAddress(), port);
			ti.computeDetails();
			NtpV3Packet pkct = ti.getMessage();
			Map<String, Number> retValues = new HashMap<String, Number>(4);
			retValues.put("RootDelay", pkct.getRootDelayInMillisDouble());
			retValues.put("RootDispersion", pkct.getRootDispersionInMillisDouble());
			retValues.put("Offset", ti.getOffset());
			retValues.put("Delay", ti.getDelay());
			return retValues;
		} catch (IOException e) {
			log(Level.ERROR, e, "NTP IO exception %s", e);
		}
		return Collections.emptyMap();
	}

	@Override
	public String getSourceType() {
		return "NTP";
	}
	
	/* (non-Javadoc)
	 * @see jrds.Probe#getUptime()
	 */
	@Override
	public long getUptime() {
		return Long.MAX_VALUE;
	}

}
