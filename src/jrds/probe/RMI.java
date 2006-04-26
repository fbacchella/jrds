package jrds.probe;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.agent.RProbe;

import org.apache.log4j.Logger;

public abstract class RMI extends Probe {
	static final private Logger logger = Logger.getLogger(RMI.class);

	private RProbe rp = null;
	private String remoteName = null;

	public RMI(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
	}
	
	protected void init(List args) {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(getHost().getName(), 2002);
			rp = (RProbe) registry.lookup(RProbe.NAME);
			remoteName = rp.prepare(getPd().getRmiClass(), args);
		} catch (RemoteException e) {
			logger.error("Remote exception on server with probe " + this + ": " + e.getCause());
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Map getNewSampleValues() {
		Map retValues = new HashMap(0);
		try {
			if(rp != null)
				retValues = rp.query(remoteName);
		} catch (RemoteException e) {
			logger.error("Remote exception on server with probe " + this + ": " + e.getCause());
		}
		return retValues;
	}
}
