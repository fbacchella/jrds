package jrds.probe;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Starter;
import jrds.agent.RProbe;

import org.apache.log4j.Logger;

public abstract class RMI extends Probe {
	static final private Logger logger = Logger.getLogger(RMI.class);
	//We want to have socket with a short default timeout
	static final private RMIClientSocketFactory sf = new RMIClientSocketFactory() {
		public Socket createSocket(String host, int port) throws IOException {
			Socket s = new Socket(host, port) {
				public void connect(SocketAddress endpoint) throws IOException {
					super.connect(endpoint, TIMEOUT * 1000);
				}
			};
			s.setSoTimeout(TIMEOUT * 1000);
			return s;
		}
	};
	List<?> args = new ArrayList<Object>(0);
	private String remoteName = null;
	static {
		System.setProperty("java.rmi.server.disableHttp", "true");
		System.setProperty("sun.rmi.transport.tcp.readTimeout", "TIMEOUT * 1000");
		System.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "TIMEOUT * 1000");
		System.setProperty("sun.rmi.transport.connectionTimeout", "TIMEOUT * 1000");
	}

	static final Map<RdsHost, Registry> registries = new HashMap<RdsHost, Registry>();
	static Starter s = new Starter() {

		@Override
		public boolean start(RdsHost host) {
			boolean started = false;
			Registry registry;
			try {
				registry = LocateRegistry.getRegistry(host.getName(), 2002, sf);
				registries.put(host, registry);
				started = true;
			} catch (RemoteException e) {
				logger.error("Remote exception on server " + host + ": " + e.getCause());
			}
			return started;
		}

		@Override
		public void stop(RdsHost host) {
			registries.remove(host);
		}

	};

	public RMI(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
		monitoredHost.addStarter(s);
	}

	protected  RProbe init() {
		Registry registry = registries.get(getHost());
		RProbe rp = null;
		if(registry != null) {
			try {
				rp = (RProbe) registry.lookup(RProbe.NAME);
				if( ! rp.exist(remoteName))
					remoteName = rp.prepare(getPd().getRmiClass(), args);
			} catch (RemoteException e) {
				rp = null;
				logger.error("Remote exception on server with probe " + this + ": " + e.getCause());
			} catch (NotBoundException e) {
				rp = null;
				logger.error("Unattended exception: ", e);
			}
		}
		return rp;
	}

	@Override
	public Map getNewSampleValues() {
		Map retValues = new HashMap(0);
		RProbe rp = init();
		try {
			if(rp != null)
				retValues = rp.query(remoteName);
		} catch (RemoteException e) {
			logger.error("Remote exception on server with probe " + this + ": " + e.getCause());
		}
		return retValues;
	}

	public List<?> getArgs() {
		return args;
	}

	public void setArgs(List<?> l) {
		this.args = l;
	}
}
