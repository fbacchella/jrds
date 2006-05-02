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

import org.rrd4j.DsType;


/**
 * @author bacchell
 *
 * TODO 
 */
public class ExaleadStatus extends HttpProbe {
	static final ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("err.adsense.connect", DsType.COUNTER, "errors.adsense.connect", 0, 100);
		pd.add("err.adsense.highload", DsType.COUNTER, "errors.adsense.highload", 0, 100);
		pd.add("err.adsense.parsing", DsType.COUNTER, "errors.adsense.parsing", 0, 100);
		pd.add("err.adsense.timeout", DsType.COUNTER, "errors.adsense.timeout", 0, 100);
		pd.add("err.exalead.decprms", DsType.COUNTER, "errors.exalead.decodingparams", 0, 100);
		pd.add("err.exalead.index", DsType.COUNTER, "errors.exalead.index", 0, 100);
		pd.add("err.google.connect", DsType.COUNTER, "errors.google.connect", 0, 100);
		pd.add("err.google.parsing", DsType.COUNTER, "errors.google.parsing", 0, 100);
		pd.add("err.untrapped", DsType.COUNTER, "errors.untrapped", 0, 100);
		pd.add("queries", DsType.COUNTER, 0, 100);
		pd.add("queries.adsense", DsType.COUNTER, 0, 100);
		pd.add("queries.google", DsType.COUNTER, 0, 100);
		pd.add("threads.adsense", DsType.GAUGE, 0, 100);
		pd.add("threads.queries", DsType.GAUGE, 0, 100);
		pd.add("uptime");
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
		return filterUpTime("uptime", retValue);
	}
}
