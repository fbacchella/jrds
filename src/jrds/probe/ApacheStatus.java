/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.ApacheRequests;
import jrds.graphe.ApacheTransfer;


/**
 * A class to probe the apache status from the /server-status URL
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ApacheStatus extends HttpProbe implements UrlProbe {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("Total Accesses", ProbeDesc.COUNTER);
		pd.add("Total kBytes", ProbeDesc.COUNTER);
		pd.add("CPULoad", ProbeDesc.GAUGE);
		pd.add("Uptime", ProbeDesc.NONE);
		pd.add("ReqPerSec", ProbeDesc.GAUGE);
		pd.add("BytesPerSec", ProbeDesc.GAUGE);
		pd.add("BytesPerReq", ProbeDesc.GAUGE);
		pd.add("BusyWorkers", ProbeDesc.GAUGE);
		pd.add("IdleWorkers", ProbeDesc.GAUGE);
		pd.setProbeName("apachestatus");
		pd.setGraphClasses(new Object[] {ApacheRequests.class, ApacheTransfer.class, "apacheworkers"});
	}

	/**
	 * @param monitoredHost
	 * @param newurl
	 * @throws MalformedURLException
	 */
	public ApacheStatus(RdsHost monitoredHost, URL newurl) throws MalformedURLException {
		super(monitoredHost, pd, new URL("http", newurl.getHost(), newurl.getPort(), "/server-status?auto"));

	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.HttpProbe#parseLines(java.util.List)
	 */
	protected Map parseLines(List<String> lines) {
		Map<String, Double> retValue = new HashMap<String, Double>(lines.size());
		for(String l: lines) {
			String[] kvp = l.split(": ");
			try {
				retValue.put(kvp[0], Double.valueOf(kvp[1]));
			}
			catch (java.lang.NumberFormatException ex) {};
		}
		return filterUpTime("Uptime", retValue);
	}
}
