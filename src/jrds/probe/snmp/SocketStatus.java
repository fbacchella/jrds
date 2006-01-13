/*
 * Created on 7 déc. 2004
 *
 * TODO 
 */
package jrds.probe.snmp;

import java.util.Collection;
import java.util.Map;

import jrds.RdsHost;

import org.jrobin.core.DsDef;
import org.jrobin.core.RrdException;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SocketStatus extends RdsSnmpSimple {

	/**
	 * @param monitoredHost
	 */
	public SocketStatus(RdsHost monitoredHost) {
		super(monitoredHost, null);
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsSnmpRrd#initNameMap()
	 */
	protected Map initNameMap() {
 
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsRrd#getView()
	 */
	public String getView() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsRrd#getCategory()
	 */
	public String getCategory() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsRrd#initName()
	 */
	protected String initName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsRrd#getDsDefs()
	 */
	protected DsDef[] getDsDefs() throws RrdException {
		DsDef[] localDs = new DsDef[12];
		localDs[0] = new DsDef("closed","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("listen","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("synSent","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("synReceived","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("established","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("finWait1","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("finWait2","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("closeWait","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("lastAck","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("closing","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("timeWait","GAUGE",600, 0, Double.NaN);
		localDs[0] = new DsDef("deleteTCB","GAUGE",600, 0, Double.NaN);
		return localDs;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.RdsRrd#initGraphList()
	 */
	protected Collection initGraphList() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
