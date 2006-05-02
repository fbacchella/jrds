/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.rstat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProgramNotRegisteredException;
import org.acplt.oncrpc.OncRpcProtocols;
import org.apache.log4j.Logger;
import org.rrd4j.DsType;

/**
 * This probe is used to collect data throught rstatd, version 3. The process in.rstatd or rstat
 * needs to run at the client
 *  
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class RstatProbe4 extends Probe {
	static final private Logger logger = Logger.getLogger(RstatProbe4.class);
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("v_pgpgin", DsType.COUNTER);
		pd.add("v_pgpgout", DsType.COUNTER);
		pd.add("v_pswpin", DsType.COUNTER);
		pd.add("v_pswpout", DsType.COUNTER);
		pd.add("v_swtch", DsType.COUNTER);
		pd.add("v_intr", DsType.COUNTER);
		pd.setProbeName("rstat");
		pd.setGraphClasses(new Object[] {"rstatvm", "rstatint"});
		
	}

	/**
	 * @param monitoredHost
	 * @param pd
	 */
	public RstatProbe4(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map retValue = new HashMap();
		 try {
		 	rstatClient c = new rstatClient(InetAddress.getByName(getHost().getName()), OncRpcProtocols.ONCRPC_UDP);
		     statsvar sv = c.RSTATPROC_STATS_4();
		     retValue.put("v_pgpgin", new Double(sv.v_pgpgin));
		     retValue.put("v_pgpgout", new Double(sv.v_pgpgout));
		     retValue.put("v_pswpin", new Double(sv.v_pswpin));
		     retValue.put("v_pswpout", new Double(sv.v_pswpout));
		     retValue.put("v_swtch", new Double(sv.v_swtch));
		     retValue.put("v_intr", new Double(sv.v_intr));
		     long bootime = sv.boottime.tv_sec + sv.boottime.tv_sec/1000000;
		     long currtime = sv.curtime.tv_sec + sv.curtime.tv_sec/1000000;
		     retValue.put("uptime", new Double(currtime - bootime));
		 } catch ( OncRpcProgramNotRegisteredException e ) {
		     logger.error("ONC/RPC program server not found: " + getHost().getName());
		 } catch ( OncRpcException e ) {
		 	logger.error("Could not contact portmapper: " + e.getMessage());
		 } catch ( IOException e ) {
		 	logger.error("Could not contact portmapper: " + e.getMessage());
		 }
		 return filterUpTime("uptime", retValue);
	}

}
