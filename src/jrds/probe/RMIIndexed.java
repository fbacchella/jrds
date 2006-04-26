package jrds.probe;

import java.util.ArrayList;
import java.util.List;

import jrds.ProbeDesc;
import jrds.RdsHost;

public class RMIIndexed extends RMI implements IndexedProbe {
	private String index;
	public RMIIndexed(RdsHost monitoredHost, ProbeDesc pd, String indexKey) {
		super(monitoredHost, pd);
		this.index = indexKey;
		List<String> l = new ArrayList<String>(1);
		l.add(indexKey);
		init(l);
	}
	public RMIIndexed(RdsHost monitoredHost, ProbeDesc pd, Integer port, Boolean local) {
		super(monitoredHost, pd);
		this.index = port.toString();
		List<Object> l = new ArrayList<Object>(2);
		l.add(port);
		l.add(local);
		init(l);
	}
	
	public String getIndexName() {
		return index;
	}
}
