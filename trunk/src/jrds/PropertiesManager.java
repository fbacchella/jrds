package jrds;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
	
	private ClassLoader doClassLoader() {
		 FileFilter filter = new  FileFilter(){
			public boolean accept(File file) {
				return  (! file.isHidden()) && file.isFile() && file.getName().endsWith(".jar");
			}
		};

		Collection<URL> urls = new HashSet<URL>();

		if(extensiondir != null) {
			logger.debug("Setting class directories to: " + extensiondir);

			File path = new File(extensiondir);

			if(path.isDirectory()) {
				for(File f: path.listFiles(filter)) {
					try {
						urls.add(f.toURL());
					} catch (MalformedURLException e) {
					}
				}
			}
		}

		for(URL u: libspath) {
			urls.add(u);
		}

		URL[] arrayUrl = new URL[urls.size()];
		urls.toArray(arrayUrl);
		return  URLClassLoader.newInstance(arrayUrl, this.getClass().getClassLoader());
	}

	public void update()
	{
		String[] levels = { "trace", "debug", "info", "error"};
		String[] defaultLevel = { "", "", "", "org.snmp4j,org.apache"};
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

		//Let's configure the log fast
		try {
			jrds.JrdsLoggerConfiguration.configure(this);
		} catch (IOException e1) {
			logger.error("Unable to set log file to " + this.logfile);
		}

		legacymode = parseBoolean(getProperty("legacymode", "1"));
		configdir = getProperty("configdir", "config");
		rrddir = getProperty("rrddir", "probe");
		step = parseInteger(getProperty("step", "300"));
		collectorThreads = parseInteger(getProperty("collectorThreads", "1"));
		dbPoolSize = parseInteger(getProperty("dbPoolSize", "10")) + collectorThreads;
		syncPeriod = parseInteger(getProperty("syncPeriod", "-1"));
		String libspathString = getProperty("libspath", "");
		if(! "".equals(libspathString)) {
			for(String libName: libspathString.split(";")) {
				File lib = new File(libName);
				if(lib.isFile() || lib.isDirectory())
					try {
						libspath.add(lib.toURL());
					} catch (MalformedURLException e) {
						logger.fatal("What is this library " + lib);
					}
					else
						logger.error("Invalid lib path: "+ libName);
			}
		}

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
		
		timeout = parseInteger(getProperty("timeout", "30"));
		rrdbackend = getProperty("rrdbackend", "NIO");
		extensionClassLoader = doClassLoader();
		String actionMailFrom = getProperty("action.mail.from", "root");
		Threshold.mailappender.setFrom(actionMailFrom);
		String actionMailTo = getProperty("action.mail.to", "root");
		Threshold.mailappender.setTo(actionMailTo);
		String actionMailHost = getProperty("action.mail.host", "localhost");
		Threshold.mailappender.setSMTPHost(actionMailHost);
		Threshold.mailappender.activateOptions();

		Locale.setDefault(new Locale("POSIX"));
	}


	public String configdir;
	public String urlpngroot;
	public String rrddir;
	public String logfile;
	public int step;
	public int collectorThreads;
	public int dbPoolSize;
	public int syncPeriod;
	public final Set<URL> libspath = new HashSet<URL>();
	public final Map<Level, List<String>> loglevels = new HashMap<Level, List<String>>();
	public Level loglevel;
	public boolean legacymode;
	public String tmpdir;
	public int timeout;
	public String rrdbackend;
	public String extensiondir;
	public ClassLoader extensionClassLoader;

}
