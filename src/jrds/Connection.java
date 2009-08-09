package jrds;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public abstract class Connection extends Starter {
	private String name;
	private long uptime;

	public abstract Object getConnection();

	Socket makeSocket(String host, int port) throws UnknownHostException, IOException {
		Socket s = new Socket(host, port) {
			public void connect(SocketAddress endpoint) throws IOException {
				super.connect(endpoint, getTimeout() * 1000);
			}
		};
		s.setSoTimeout(getTimeout());
		return s;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#getKey()
	 */
	@Override
	public Object getKey() {
		if(name !=null)
			return name;
		else
			return this.getClass().getName();
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
			return ((RdsHost)getParent()).getName();
		}
		if(getParent() instanceof Probe) {
			return ((Probe)getParent()).getHost().getName();
		}
		return null;
	}

	/**
	 * To get the default time out
	 * @return the connection timeout in second
	 */
	public int getTimeout() {
		return HostsList.getRootGroup().getTimeout();

	}

	/* (non-Javadoc)
	 * @see jrds.Starter#start()
	 */
	@Override
	public boolean start() {
		boolean started =  startConnection();
		if(started)
			uptime = setUptime();
		return started;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#stop()
	 */
	@Override
	public void stop() {
		stopConnection();
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
	
}
