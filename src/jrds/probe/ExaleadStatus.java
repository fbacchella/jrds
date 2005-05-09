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
public class ExaleadStatus extends HttpProbe {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("errors.adsense.connect", ProbeDesc.COUNTER);
		pd.add("errors.adsense.highload", ProbeDesc.COUNTER);
		pd.add("errors.adsense.parsing", ProbeDesc.GAUGE);
		pd.add("errors.adsense.timeout", ProbeDesc.GAUGE);
		pd.add("errors.exalead.decodingparams", ProbeDesc.GAUGE);
		pd.add("errors.exalead.index", ProbeDesc.GAUGE);
		pd.add("errors.google.connect", ProbeDesc.GAUGE);
		pd.add("errors.google.parsing", ProbeDesc.GAUGE);
		pd.add("errors.untrapped", ProbeDesc.GAUGE);
		pd.add("queries", ProbeDesc.GAUGE);
		pd.add("queries.adsense", ProbeDesc.GAUGE);
		pd.add("queries.google", ProbeDesc.GAUGE);
		pd.add("threads.adsense", ProbeDesc.GAUGE);
		pd.add("threads.queries", ProbeDesc.GAUGE);
		pd.add("uptime", ProbeDesc.NONE);
		pd.setRrdName("apachestatus");
		pd.setGraphClasses(new Object[] {"exaleaderrors.xml"});
		
	}

	/**
	 * @param monitoredHost
	 * @param newurl
	 * @throws MalformedURLException
	 */
	public ExaleadStatus(RdsHost monitoredHost, URL newurl) throws MalformedURLException {
		super(monitoredHost, pd, new URL("http", newurl.getHost(), newurl.getPort(), "/stat"));

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
		return retValue;
	}
}
