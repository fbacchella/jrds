package jrds.starter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jrds.Probe;
import jrds.RdsHost;

import org.apache.log4j.Level;

public class Resolver extends Starter {
	private final String hostname;
	InetAddress address = null;

	public Resolver(String hostname) {
		this.hostname = hostname;
        log(Level.DEBUG, "New dns resolver with name %s", hostname);
	}

	@Override
	public boolean start() {
		boolean started = false;
		try {
			address = InetAddress.getByName(hostname);
			started = true;
	        log(Level.TRACE, "%s resolved to %s", hostname, address.getHostAddress());

		} catch (UnknownHostException e) {
			log(Level.ERROR, e,  "DNS host name %s can't be found", hostname);
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
		else if(node instanceof Probe<?,?>) {
			Probe<?,?> p = (Probe<?,?>) node;
			host = p.getHost();
		}
		else {
			return null;
		}
		return "resolver:" + host.getDnsName();
	}

	@Deprecated
	public static Object makeKey(String hostname) {
		return "resolver:" + hostname;
	}
}
