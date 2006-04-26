package jrds.probe;

import java.util.ArrayList;

import jrds.ProbeDesc;
import jrds.RdsHost;

public class RMISimple extends RMI {
	public RMISimple(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
		init(new ArrayList(0));
	}
}
