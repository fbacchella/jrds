/*
 * Created on 24 déc. 2004
 *
 * TODO 
 */
package jrds.probe.snmp;

import java.util.Set;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.SnmpProbe;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class RdsSnmpSimple extends SnmpProbe  {
	static final private Logger logger = JrdsLogger.getLogger(RdsSnmpSimple.class);

	public RdsSnmpSimple(RdsHost monitoredHost, ProbeDesc pd) {
		super(monitoredHost, pd);
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.snmp.SnmpPreparator#makeOidSet()
	 */
	public Set getOidSet() {
		return getOidNameMap().keySet();
	}
}
