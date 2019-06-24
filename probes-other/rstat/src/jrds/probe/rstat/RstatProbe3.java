/*##########################################################################
_##
_##  $Id: RstatProbe3.java 365 2006-09-02 12:04:34Z fbacchella $
_##
_##########################################################################*/

package jrds.probe.rstat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.Probe;
import jrds.starter.Resolver;

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProgramNotRegisteredException;
import org.acplt.oncrpc.OncRpcProtocols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This probe is used to collect data throught rstatd, version 3. The process in.rstatd or rstat
 * needs to run at the client
 *  
 * @author Fabrice Bacchella
 * @version $Revision: 365 $
 */
public class RstatProbe3 extends Probe<String, Number> {

	public void configure() {
		
	}
	
	/* (non-Javadoc)
	 * @see jrds.Probe#getNewSampleValues()
	 */
	public Map<String, Number> getNewSampleValues() {
		Resolver r = find(Resolver.class);
		if(! r.isStarted()) {
			return Collections.emptyMap();
		}
		Map<String,Number> retValue = new HashMap<String,Number>();
		try {
			rstatClient c = new rstatClient(r.getInetAddress(), OncRpcProtocols.ONCRPC_UDP);
			statstime st = c.RSTATPROC_STATS_3();
			retValue.put("v_pgpgin", new Double(st.v_pgpgin));
			retValue.put("v_pgpgout", new Double(st.v_pgpgout));
			retValue.put("v_pswpin", new Double(st.v_pswpin));
			retValue.put("v_pswpout", new Double(st.v_pswpout));
			retValue.put("v_swtch", new Double(st.v_swtch));
			retValue.put("v_intr", new Double(st.v_intr));
			long bootime = st.boottime.tv_sec + st.boottime.tv_sec/1000000;
			long currtime = st.curtime.tv_sec + st.curtime.tv_sec/1000000;
			setUptime(currtime - bootime);
		} catch ( OncRpcProgramNotRegisteredException e ) {
			log(Level.ERROR, "ONC/RPC program server not found");
		} catch ( OncRpcException e ) {
			log(Level.ERROR, e, "Could not contact portmapper: %s", e.getMessage());
		} catch ( IOException e ) {
			log(Level.ERROR, e, "Could not contact portmapper: %s", e.getMessage());
		}
		return retValue;
	}
	@Override
	public String getSourceType() {
		return "rstat v3";
	}

}
