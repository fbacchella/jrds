package jrds.starter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jrds.Probe;
import jrds.RdsHost;

import org.apache.log4j.Logger;

public class Resolver extends Starter {
	static final private Logger logger = Logger.getLogger(Resolver.class);
	String hostname = "";
	InetAddress address = null;

	public Resolver(String hostname) {
		logger.debug("New dns resolver for " + hostname);
		this.hostname = hostname;
	}

	@Override
	public boolean start() {
		boolean started = false;
		try {
			address = InetAddress.getByName(hostname);
			started = true;
		} catch (UnknownHostException e) {
			logger.error("DNS host name " + hostname + " can't be found");
		}
		return started;
	}
	
	@Override
	public void stop() {
		address = null;
	}

	public InetAddress getInetAddress() {
		return address;
	}
	
	@Override
	public Object getKey() {
		return "resolver:" + hostname;
	}

	public static Object makeKey(StarterNode node) {
		RdsHost host = null;
		if(node instanceof RdsHost)
			host =(RdsHost)node;
		else if(node instanceof Probe) {
			Probe p = (Probe) node;
			host = p.getHost();
		}
		return "resolver:" + host.getDnsName();
	}

	public static Object makeKey(String hostname) {
		return "resolver:" + hostname;
	}
}
