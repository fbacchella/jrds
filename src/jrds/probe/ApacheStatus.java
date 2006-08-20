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

import jrds.RdsHost;

/**
 * A class to probe the apache status from the /server-status URL
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ApacheStatus extends HttpProbe implements UrlProbe {
	private int port = 80;
	/**
	 * @param monitoredHost
	 * @param newurl
	 * @throws MalformedURLException
	 */
	public ApacheStatus(URL newurl) throws MalformedURLException {
		super(new URL("http", newurl.getHost(), newurl.getPort(), "/server-status?auto"));
		this.port = newurl.getPort();
		if(port <= 0)
			port = 80;
	}

	public ApacheStatus(Integer port) throws MalformedURLException {
		this.port = port;
	}

	public ApacheStatus() throws MalformedURLException {
	}

	@Override
	public String getIndexName() {
		return Integer.toString(port);
	}

	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		try {
			setUrl(new URL("http", monitoredHost.getName(), port, "/server-status?auto"));
		} catch (MalformedURLException e) {
		}
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
