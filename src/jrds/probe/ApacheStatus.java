/*
 * Created on 6 janv. 2005
 *
 * TODO 
 */
package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.ApacheRequests;
import jrds.graphe.ApacheTransfer;


/**
 * @author bacchell
 *
 * TODO 
 */
public class ApacheStatus extends HttpProbe {
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
		pd.setRrdName("apachestatus");
		pd.setGraphClasses(new Class[] {ApacheRequests.class, ApacheTransfer.class});
		
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
