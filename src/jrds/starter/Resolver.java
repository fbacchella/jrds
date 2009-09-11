package jrds.starter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jrds.RdsHost;

import org.apache.log4j.Logger;

public class Resolver extends Starter {
	static final private Logger logger = Logger.getLogger(Resolver.class);
	String hostname = "";
	InetAddress address = null;

	public Resolver(String hostname) {
		super();
		this.hostname = hostname;
	}

	public Resolver() {
		super();
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public boolean start() {
		boolean started = false;
		try {
			address = InetAddress.getByName(hostname);
			started = true;
		} catch (UnknownHostException e) {
			logger.error("Host name " + hostname + " can't be solved");
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

	public static Object buildKey(String hostname) {
		return "resolver:" + hostname;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#register(jrds.StarterNode)
	 */
	@Override
	public Starter register(StarterNode node) {
		StartersSet ss = node.getStarters();
		if(ss.find(getKey()) == null)
			super.register(node);
		return ss.find(getKey());
	}

	public static Object makeKey(StarterNode node) {
		RdsHost host =(RdsHost)node;
		return "resolver:" + host.getDnsName();
	}
}
