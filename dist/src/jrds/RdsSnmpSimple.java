/*
 * Created on 24 déc. 2004
 *
 * TODO 
 */
package jrds;

import java.util.Set;

import jrds.snmp.SnmpRequester;

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
	 * @see com.aol.jrds.RdsSnmpRrd#getSnmpRequester()
	 */
	protected SnmpRequester getSnmpRequester() {
		return SnmpRequester.SIMPLE;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.snmp.SnmpPreparator#makeOidSet()
	 */
	public Set getOidSet() {
		return getOidNameMap().keySet();
	}
}
