/*
 * Created on 6 janv. 2005
 *
 * TODO 
 */
package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
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
		pd.add("err.adsense.connect", ProbeDesc.COUNTER, "errors.adsense.connect", 0, 100);
		pd.add("err.adsense.highload", ProbeDesc.COUNTER, "errors.adsense.highload", 0, 100);
		pd.add("err.adsense.parsing", ProbeDesc.COUNTER, "errors.adsense.parsing", 0, 100);
		pd.add("err.adsense.timeout", ProbeDesc.COUNTER, "errors.adsense.timeout", 0, 100);
		pd.add("err.exalead.decprms", ProbeDesc.COUNTER, "errors.exalead.decodingparams", 0, 100);
		pd.add("err.exalead.index", ProbeDesc.COUNTER, "errors.exalead.index", 0, 100);
		pd.add("err.google.connect", ProbeDesc.COUNTER, "errors.google.connect", 0, 100);
		pd.add("err.google.parsing", ProbeDesc.COUNTER, "errors.google.parsing", 0, 100);
		pd.add("err.untrapped", ProbeDesc.COUNTER, "errors.untrapped", 0, 100);
		pd.add("queries", ProbeDesc.COUNTER, 0, 100);
		pd.add("queries.adsense", ProbeDesc.COUNTER, 0, 100);
		pd.add("queries.google", ProbeDesc.COUNTER, 0, 100);
		pd.add("threads.adsense", ProbeDesc.GAUGE, 0, 100);
		pd.add("threads.queries", ProbeDesc.GAUGE, 0, 100);
		pd.add("uptime", ProbeDesc.NONE);
		pd.setProbeName("exaleadstatus");
		pd.setGraphClasses(new Object[] {"exaleaderrors", "exaleadqueries", "exaleadthreads"});
		
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
	protected Map parseLines(List<String> lines) {
		Map<String, Number> retValue = new HashMap<String, Number>(lines.size());
		for(String l: lines) {
			String[] kvp = l.split(": ");
			try {
				retValue.put(kvp[0], Double.valueOf(kvp[1]));
			}
			catch (java.lang.NumberFormatException ex) {};
		}
		return filterUpTime("uptime", retValue);
	}
}
