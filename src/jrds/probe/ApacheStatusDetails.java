/*##########################################################################
 _##
 _##  $Id: ApacheStatus.java 475 2009-05-15 20:04:03Z fbacchella $
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

import org.apache.log4j.Logger;

/**
 * A class to probe the apache status from the /server-status URL
 * @author Fabrice Bacchella 
 * @version $Revision: 475 $,  $Date: 2009-05-15 22:04:03 +0200 (Fri, 15 May 2009) $
 */
public class ApacheStatusDetails extends HttpProbe implements IndexedProbe {

	//"_" Waiting for Connection, "S" Starting up, "R" Reading Request,
	//"W" Sending Reply, "K" Keepalive (read), "D" DNS Lookup,
	//"C" Closing connection, "L" Logging, "G" Gracefully finishing,
	//"I" Idle cleanup of worker, "." Open slot with no current process
	static private final Map<Character, WorkerStat> map = new HashMap<Character, WorkerStat>();
	enum WorkerStat {
		WAITING('_'),
		STARTING('S'),
		READING('R'),
		SENDING('W'),
		KEEPALIVE('K'),
		DNS('D'),
		CLOSING('C'),
		LOGGING('L'),
		GRACEFULLY('G'),
		IDLE('I'),
		OPEN('.');


		static WorkerStat resolv(char key) {
			return map.get(key);
		}
		static synchronized void add(char key, WorkerStat value) {
			map.put(key, value);
		}
		WorkerStat(char key) {
			WorkerStat.add(key, this);
		}
	}

	static final private Logger logger = Logger.getLogger(ApacheStatusDetails.class);

	/**
	 * @param monitoredHost
	 * @param newurl
	 * @throws MalformedURLException
	 */
	public ApacheStatusDetails(URL newurl) throws MalformedURLException {
		super(newurl);
	}

	public ApacheStatusDetails(Integer port) throws MalformedURLException {
		super(new URL("http", EMPTYHOST, port, "/server-status?auto"));
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
		logger.debug("Getting " + getUrl());
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			List<String> lines = new ArrayList<String>();
			String lastLine;
			while((lastLine = in.readLine()) != null)
				lines.add(lastLine);
			in.close();
			vars = parseLines(lines);
		} catch (IOException e) {
			logger.error("Unable to read url " + getUrl() + " because: " + e.getMessage());
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
				if("Scoreboard".equals(kvp[0].trim())) {
					parseScoreboard(kvp[1].trim(), retValue);
				}
				else
					retValue.put(kvp[0], Double.valueOf(kvp[1]));
			}
			catch (java.lang.NumberFormatException ex) {};
		}
		Number uptimeNumber = retValue.remove("Uptime");
		if(uptimeNumber != null)
			setUptime(uptimeNumber.longValue());
		return retValue;
	}

	void parseScoreboard(String scoreboard, Map<String, Number> retValue) {
		int workers[] = new int[WorkerStat.values().length];
		for(char c: scoreboard.toCharArray()) {
			WorkerStat worker = WorkerStat.resolv(c);
			workers[worker.ordinal()]++;
		}
		for(WorkerStat worker: WorkerStat.values()) {
			retValue.put(worker.toString(), workers[worker.ordinal()]);
		}

	}
}
