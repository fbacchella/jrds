package jrds.probe;

import java.util.ArrayList;
import java.util.List;

public class RMIIndexed extends RMI implements IndexedProbe {
	private String index;
	public RMIIndexed(String indexKey) {
		super();
		this.index = indexKey;
		List<String> l = new ArrayList<String>(1);
		l.add(indexKey);
		setArgs(l);
	}
	public RMIIndexed(Integer port, Boolean local) {
		super();
		this.index = port.toString();
		List<Object> l = new ArrayList<Object>(2);
		l.add(port);
		l.add(local);
		setArgs(l);
	}

	public String getIndexName() {
		return index;
	}
}
