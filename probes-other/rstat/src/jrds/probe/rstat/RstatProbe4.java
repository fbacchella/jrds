/*##########################################################################
_##
_##  $Id: RstatProbe4.java 365 2006-09-02 12:04:34Z fbacchella $
_##
_##########################################################################*/

package jrds.probe.rstat;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.objects.probe.Probe;
import jrds.starter.Resolver;

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProgramNotRegisteredException;
import org.acplt.oncrpc.OncRpcProtocols;
import org.apache.log4j.Level;

/**
 * This probe is used to collect data throught rstatd, version 3. The process in.rstatd or rstat
 * needs to run at the client
 *  
 * @author Fabrice Bacchella
 * @version $Revision: 365 $
 */
public class RstatProbe4 extends Probe<String, Number> {

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
		Map<String,Number> retValue = new HashMap<String,Number>(7);
		try {
			rstatClient c = new rstatClient(r.getInetAddress(), OncRpcProtocols.ONCRPC_UDP);
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
			log(Level.ERROR, "ONC/RPC program server not found");
		} catch ( OncRpcException e ) {
			log(Level.ERROR, e, "Could not contact portmapper %s", e.getMessage());
		} catch ( IOException e ) {
			log(Level.ERROR, e, "Could not contact portmapper %s", e.getMessage());
		}
		return retValue;
	}

	@Override
	public String getSourceType() {
		return "rstat v4";
	}

}
