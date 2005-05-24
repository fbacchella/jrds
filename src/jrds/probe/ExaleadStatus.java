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


/**
 * @author bacchell
 *
 * TODO 
 */
public class ExaleadStatus extends HttpProbe {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("err.adsense.connect", ProbeDesc.COUNTER, "errors.adsense.connect");
		pd.add("err.adsense.highload", ProbeDesc.COUNTER, "errors.adsense.highload");
		pd.add("err.adsense.parsing", ProbeDesc.COUNTER, "errors.adsense.parsing");
		pd.add("err.adsense.timeout", ProbeDesc.COUNTER, "errors.adsense.timeout");
		pd.add("err.exalead.decprms", ProbeDesc.COUNTER, "errors.exalead.decodingparams");
		pd.add("err.exalead.index", ProbeDesc.COUNTER, "errors.exalead.index");
		pd.add("err.google.connect", ProbeDesc.COUNTER, "errors.google.connect");
		pd.add("err.google.parsing", ProbeDesc.COUNTER, "errors.google.parsing");
		pd.add("err.untrapped", ProbeDesc.COUNTER, "errors.untrapped");
		pd.add("queries", ProbeDesc.COUNTER);
		pd.add("queries.adsense", ProbeDesc.COUNTER);
		pd.add("queries.google", ProbeDesc.COUNTER);
		pd.add("threads.adsense", ProbeDesc.GAUGE);
		pd.add("threads.queries", ProbeDesc.GAUGE);
		pd.add("uptime", ProbeDesc.NONE);
		pd.setRrdName("exaleadstatus");
		pd.setGraphClasses(new Object[] {"exaleaderrors.xml", "exaleadqueries.xml", "exaleadthreads.xml"});
		
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
