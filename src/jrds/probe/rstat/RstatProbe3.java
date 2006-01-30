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

/**
 * This probe is used to collect data throught rstatd, version 3. The process in.rstatd or rstat
 * needs to run at the client
 *  
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class RstatProbe3 extends Probe {
	static final private Logger logger = Logger.getLogger(RstatProbe3.class);
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("v_pgpgin", ProbeDesc.COUNTER);
		pd.add("v_pgpgout", ProbeDesc.COUNTER);
		pd.add("v_pswpin", ProbeDesc.COUNTER);
		pd.add("v_pswpout", ProbeDesc.COUNTER);
		pd.add("v_swtch", ProbeDesc.COUNTER);
		pd.add("v_intr", ProbeDesc.COUNTER);
		pd.setName("rstat");
		pd.setGraphClasses(new Object[] {"rstatvm.xml", "rstatint.xml"});
		
	}

	/**
	 * @param monitoredHost
	 * @param pd
	 */
	public RstatProbe3(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map retValue = new HashMap();
		 try {
		 	rstatClient c = new rstatClient(InetAddress.getByName(getHost().getName()), OncRpcProtocols.ONCRPC_UDP);
		     statstime st = c.RSTATPROC_STATS_3();
		     retValue.put("v_pgpgin", new Double(st.v_pgpgin));
		     retValue.put("v_pgpgout", new Double(st.v_pgpgout));
		     retValue.put("v_pswpin", new Double(st.v_pswpin));
		     retValue.put("v_pswpout", new Double(st.v_pswpout));
		     retValue.put("v_swtch", new Double(st.v_swtch));
		     retValue.put("v_intr", new Double(st.v_intr));
		     long bootime = st.boottime.tv_sec + st.boottime.tv_sec/1000000;
		     long currtime = st.curtime.tv_sec + st.curtime.tv_sec/1000000;
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
