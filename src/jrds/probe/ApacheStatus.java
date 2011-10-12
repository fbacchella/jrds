/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.objects.probe.IndexedProbe;

import org.apache.log4j.Level;

/**
 * A class to probe the apache status from the /server-status URL
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ApacheStatus extends HttpProbe implements IndexedProbe {

	public void configure(Integer port) {
		try {
			configure(new URL("http", getHost().getDnsName(), port, "/server-status?auto"));
		} catch (MalformedURLException e) {
			throw new RuntimeException("MalformedURLException",e);
		}
	}

	/**
	 * @return Returns the url.
	 */
	public String getUrlAsString() {
		String retValue = "";
		try {
			URL tempUrl = new URL("http", getUrl().getHost(), getUrl().getPort(), "/");
			retValue = tempUrl.toString();
		} catch (MalformedURLException e) {
		}
		return retValue;
	}

	public String getIndexName() {
		int port = getUrl().getPort();
		if(port <= 0)
			port = 80;
		return Integer.toString(port);
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#parseStream(java.io.InputStream)
	 */
	@Override
	protected Map<String, Number> parseStream(InputStream stream) {
		Map<String, Number> vars = java.util.Collections.emptyMap();
		log(Level.DEBUG,"Getting %s", getUrl());
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			List<String> lines = new ArrayList<String>();
			String lastLine;
			while((lastLine = in.readLine()) != null)
				lines.add(lastLine);
			in.close();
			vars = parseLines(lines);
		} catch (IOException e) {
			log(Level.ERROR,e ,  "Unable to read url %s because %s", getUrl(), e.getMessage());
		}
		return vars;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.HttpProbe#parseLines(java.util.List)
	 */
	protected Map<String, Number> parseLines(List<String> lines) {
		Map<String, Number> retValue = new HashMap<String, Number>(lines.size());
		for(String l: lines) {
			String[] kvp = l.split(": ");
			try {
				retValue.put(kvp[0], Double.valueOf(kvp[1]));
			}
			catch (java.lang.NumberFormatException ex) {};
		}
		Number uptimeNumber = retValue.remove("Uptime");
		if(uptimeNumber != null)
			setUptime(uptimeNumber.longValue());
		return retValue;
	}
}
