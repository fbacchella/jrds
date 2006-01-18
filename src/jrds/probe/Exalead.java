package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jrds.JrdsLogger;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class Exalead extends Probe {
	static final private Logger logger = Logger.getLogger(Exalead.class);

	static private final Pattern filter = Pattern.compile("(\\p{Alnum}+)=(\\p{Digit}+)");
	static private final Pattern lastarted = Pattern.compile("last-started=\"([^\"]+)\"");
	static private final int MONITORPORT = 9999;
	static private final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	int port;

	/**
	 * @param monitoredHost
	 * @param pd
	 */
	public Exalead(RdsHost monitoredHost, ProbeDesc pd, int port) {
		super(monitoredHost, pd);
		this.port = port;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		
		Map retValue = new HashMap();
		String read = getExaStatus(port);
		Matcher result = filter.matcher(read);
		while(result.find()) {
			String name = result.group(1).trim();
			Double value = new Double(result.group(2).trim());
			logger.debug(name + "=" + value);
			if(name != null && value != null)
				retValue.put(name, value);
			
		}
		retValue.put("last-started", new Long(getUptime()));
		return filterUpTime("last-started", retValue);
	}
	
	private long getUptime(){
		long uptime = 0;
		String read = getExaStatus(MONITORPORT);
		Matcher datematch = lastarted.matcher(read);
		if(datematch.find()) {
			String readdate = datematch.group(1);
			try {
				uptime = (System.currentTimeMillis()  - dateformat.parse(readdate).getTime()) / 1000;
			} catch (ParseException e) {
				logger.error("Unable to parse to a date:" + readdate);
			}
		}
		return uptime;
	}
	
	private String getExaStatus(int port) {
		logger.debug("Gettint Exalead status on " + getHost().getName() +":" + port);
		Socket so = null;
		PrintWriter out = null;
		BufferedReader in = null;
		String read = "";
		try {
			so = new Socket(getHost().getName(), port);
			out = new PrintWriter(so.getOutputStream());
			in = new BufferedReader(new InputStreamReader(so.getInputStream()));
			out.print("status\n");
			out.flush();
			char[] buff = new char[8192];
			int pos;
			while( (pos = in.read(buff, 0, buff.length)) != -1  && buff[pos - 1] != '\0') {
				read = read.concat(String.copyValueOf(buff));
			}
			read = read.concat(String.copyValueOf(buff));
		} catch (UnknownHostException e) {
			logger.error("Unknown host: " + getHost().getName());
		} catch (IOException e) {
			logger.error("host " + getHost().getName() + " unreachable");
			
		}
		return read;
	}

}
