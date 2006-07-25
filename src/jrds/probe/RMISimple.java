package jrds.probe;

import jrds.ProbeDesc;
import jrds.RdsHost;

public class RMISimple extends RMI {
	public RMISimple(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
	}
}
