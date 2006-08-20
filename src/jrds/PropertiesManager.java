package jrds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An less ugly class suposed to manage properties
 * should be reworked
 * @author Fabrice Bacchella
 */
public class PropertiesManager extends Properties {
	private final Logger logger = Logger.getLogger(PropertiesManager.class);

	public PropertiesManager()
	{
		join(System.getProperties());
		update();
	}

	public PropertiesManager(File propFile)
	{
		join(propFile);
		update();
	}

	private String getParameter(String key, String defaultValue,
			boolean doTrim) {
		String returnValue = getProperty(key);
		if (doTrim && returnValue != null) {
			returnValue = returnValue.trim();
		}
		if (returnValue == null) {
			returnValue = defaultValue;
			setProperty(key, defaultValue);
		}
		return returnValue;
	}

	private String getParameter(String s, String s1) {
		return getParameter(s, s1, true);
	}

	private int parseInteger(String s) throws NumberFormatException {
		Integer integer = null;
		if (s != null) {
			if (s.startsWith("#")) {
				integer = Integer.valueOf(s.substring(1), 16);
			}
			else
				if (s.startsWith("0x")) {
					integer = Integer.valueOf(s.substring(2), 16);
				}
				else
					if (s.startsWith("0") && s.length() > 1) {
						integer = Integer.valueOf(s.substring(1), 8);
					}
					else {
						integer = Integer.valueOf(s);
					}
		}
		return integer.intValue();
	}

	private List<String> parseLogLevel(String value) {
		return Arrays.asList(value.split(","));
	}

	public void join(URL url) {
		try {
			InputStream inputstream = url.openStream();
			load(inputstream);
			inputstream.close();
		}
		catch (IOException ex) {
			logger.warn("Invalid URL: " + ex.getLocalizedMessage());
		}
	}

	public void join(Properties moreProperties) {
		putAll(moreProperties);
	}

	public void join(File propFile)
	{
		logger.debug("Using propertie file " + propFile.getAbsolutePath());
		try {
			InputStream inputstream = new FileInputStream(propFile);
			load(inputstream);
			inputstream.close();
		} catch (IOException ex) {
			logger.warn("Invalid properties file " + propFile.getAbsolutePath() + ": " + ex.getLocalizedMessage());
		}
	}

	public void join(InputStream propStream)
	{
		try {
			load(propStream);
		} catch (IOException ex) {
			logger.warn("Invalid properties stream " + propStream + ": " + ex);
		}
	}

	public void update()
	{
		configdir = getParameter("configdir", "config");
		rrddir = getParameter("rrddir", "probe");
		resolution = parseInteger(getParameter("resolution", "300"));
		collectorThreads = parseInteger(getParameter("collectorThreads", "1"));
		dbPoolSize = parseInteger(getParameter("dbPoolSize", "10"));
		syncPeriod = parseInteger(getParameter("syncPeriod", "-1"));
		libspath = getParameter("libspath", "");
		String[] levels = { "trace", "debug", "info", "error"};
		String[] defaultLevel = { "", "", "", "org.snmp4j,org.apache"};
		for(int i = 0; i < levels.length; i++) {
			String ls = levels[i];
			Level l = Level.toLevel(ls);
			String param = getParameter("log." + ls, defaultLevel[i]);
			if(! "".equals(param)) {
				List<String> loggerList = parseLogLevel(param);
				loglevels.put(l, loggerList);
			}
		}
		loglevel = Level.toLevel(getParameter("loglevel", "info"));
		logfile = getParameter("logfile", "");
	}

	public String configdir;
	public String urlpngroot;
	public String rrddir;
	public String logfile;
	public int resolution;
	public int collectorThreads;
	public int dbPoolSize;
	public int syncPeriod;
	public String libspath;
	public Map<Level, List<String>> loglevels = new HashMap<Level, List<String>>();
	public Level loglevel;

}
