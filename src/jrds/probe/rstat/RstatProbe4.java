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

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProgramNotRegisteredException;
import org.acplt.oncrpc.OncRpcProtocols;
import org.apache.log4j.Logger;

/**
 * This probe is used to collect data throught rstatd, version 3. The process in.rstatd or rstat
 * needs to run at the client
 *  
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class RstatProbe4 extends Probe {
	static final private Logger logger = Logger.getLogger(RstatProbe4.class);

	/* (non-Javadoc)
	 * @see jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map<String,Double> retValue = new HashMap<String,Double>(7);
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
		     setUptime(currtime - bootime);
		 } catch ( OncRpcProgramNotRegisteredException e ) {
		     logger.error("ONC/RPC program server not found: " + getHost().getName());
		 } catch ( OncRpcException e ) {
		 	logger.error("Could not contact portmapper: " + e.getMessage());
		 } catch ( IOException e ) {
		 	logger.error("Could not contact portmapper: " + e.getMessage());
		 }
		 return retValue;
	}

	@Override
	public String getSourceType() {
		return "rstat v4";
	}

}
