/*
 * Created on 24 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.util.Set;

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
