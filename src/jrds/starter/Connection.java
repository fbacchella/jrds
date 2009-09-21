package jrds.starter;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import jrds.Probe;
import jrds.RdsHost;

import org.apache.log4j.Logger;

public abstract class Connection extends Starter {
	static final private Logger logger = Logger.getLogger(Connection.class);
	
	private String name;
	private long uptime;

	public abstract Object getConnection();

	Socket makeSocket(String host, int port) throws UnknownHostException, IOException {
		SocketFactory sf = (SocketFactory) getParent().getStarters().find(SocketFactory.makeKey(getParent()));
		return sf.createSocket(host, port);
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#getKey()
	 */
	@Override
	public Object getKey() {
		if(name !=null)
			return name;
		else
			return getClass().getName();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the host name associated
	 * @return
	 */
	public String getHostName() {
		if(getParent() instanceof RdsHost) {
			return ((RdsHost)getParent()).getDnsName();
		}
		if(getParent() instanceof Probe) {
			return ((Probe)getParent()).getHost().getDnsName();
		}
		return null;
	}

	/**
	 * To get the default time out
	 * @return the connection timeout in second
	 */
	public int getTimeout() {
		SocketFactory sf = (SocketFactory) getParent().getStarters().find(SocketFactory.makeKey(getParent()));
		return sf.getTimeout();

	}

	/* (non-Javadoc)
	 * @see jrds.Starter#start()
	 */
	@Override
	public boolean start() {
		long begin = new Date().getTime();
		boolean started =  startConnection();
		long end = new Date().getTime();
		if(started)
			uptime = setUptime();
		logger.debug("Starting connection " + getKey() + " for " + getHostName() +" took "  + (end - begin) + "ms");
		return started;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#stop()
	 */
	@Override
	public void stop() {
		long begin = new Date().getTime();
		stopConnection();
		long end = new Date().getTime();
		logger.debug("Stopped connection " + getKey() + " for " + getHostName() +" took "  + (end - begin) + "ms");
	}

	public abstract boolean startConnection();
	public abstract void stopConnection();
	/**
	 * Return the uptime of the end point of the connexion
	 * it's called once after the connexion start
	 * It should be in seconds
	 * @return
	 */
	public abstract long setUptime();

	/**
	 * @return the uptime
	 */
	public long getUptime() {
		return uptime;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#toString()
	 */
	@Override
	public String toString() {
		return getKey() + "@" + getHostName();
	}
	
}
