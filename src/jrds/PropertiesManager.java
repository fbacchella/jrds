package jrds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
		join(System.getProperties());
		join(propFile);
		update();
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

	private boolean parseBoolean(String s)
	{
		s = s.toLowerCase().trim();
		boolean retValue = false;
		if("1".equals(s))
			retValue = true;
		else if("yes".equals(s))
			retValue = true;
		else if("y".equals(s))
			retValue = true;
		else if("true".equals(s))
			retValue = true;
		else if("enable".equals(s))
			retValue = true;
		else if("on".equals(s))
			retValue = true;

		return retValue;
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
		legacymode = parseBoolean(getProperty("legacymode", "1"));
		configdir = getProperty("configdir", "config");
		rrddir = getProperty("rrddir", "probe");
		resolution = parseInteger(getProperty("resolution", "300"));
		collectorThreads = parseInteger(getProperty("collectorThreads", "1"));
		dbPoolSize = parseInteger(getProperty("dbPoolSize", "10"));
		syncPeriod = parseInteger(getProperty("syncPeriod", "-1"));
		libspath = getProperty("libspath", "");
		tmpdir = getProperty("tmpdir", "/var/tmp/jrds");
		File tmpDirFile = new File(tmpdir);
		if( ! tmpDirFile.exists()) {
			if ( !tmpDirFile.mkdirs()) {
				logger.error(tmpdir + " doesn't exists and can't be created");
			}
		}
		else if( ! tmpDirFile.isDirectory()) {
			logger.error(tmpdir + " exists but is not a file");
		}
		else if( ! tmpDirFile.canWrite()) {
			logger.error(tmpdir + " exists can not be written");
			
		}
		String[] levels = { "trace", "debug", "info", "error"};
		String[] defaultLevel = { "", "", "", "org.snmp4j,org.apache,org.apache.commons.digester.Digester.sax"};
		for(int i = 0; i < levels.length; i++) {
			String ls = levels[i];
			Level l = Level.toLevel(ls);
			String param = getProperty("log." + ls, defaultLevel[i]);
			if(! "".equals(param)) {
				List<String> loggerList = parseLogLevel(param);
				loglevels.put(l, loggerList);
			}
		}
		loglevel = Level.toLevel(getProperty("loglevel", "info"));
		logfile = getProperty("logfile", "");
		timeout = parseInteger(getProperty("timeout", "30"));
		Locale.setDefault(new Locale("POSIX"));
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
	public boolean legacymode;
	public String tmpdir;
	public int timeout;

}
