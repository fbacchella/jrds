/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.rrd4j.DsType;

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
		pd.add("Total Accesses", DsType.COUNTER);
		pd.add("Total kBytes", DsType.COUNTER);
		pd.add("CPULoad", DsType.GAUGE);
		pd.add("Uptime");
		pd.add("ReqPerSec", DsType.GAUGE);
		pd.add("BytesPerSec", DsType.GAUGE);
		pd.add("BytesPerReq", DsType.GAUGE);
		pd.add("BusyWorkers", DsType.GAUGE);
		pd.add("IdleWorkers", DsType.GAUGE);
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
	protected Map parseLines(List lines) {
		Map retValue = new HashMap(lines.size());
		for(Iterator i = lines.iterator(); i.hasNext() ;) {
			String[] kvp = ((String) i.next()).split(": ");
			try {
				Double value = new Double(kvp[1]);
				retValue.put(kvp[0], Double.valueOf(kvp[1]));
			}
			catch (java.lang.NumberFormatException ex) {};
		}
		return filterUpTime("Uptime", retValue);
	}
}
