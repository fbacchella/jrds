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
 * @version $Revision$,  $Date$
 */
public class PropertiesManager extends Properties {
	private final Logger logger = Logger.getLogger(PropertiesManager.class);

	public PropertiesManager()
	{
		update();
	}

	public PropertiesManager(File propFile)
	{
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

	private ClassLoader doClassLoader(String extendedclasspath) {
		FileFilter filter = new  FileFilter(){
			public boolean accept(File file) {
				return  (! file.isHidden()) && file.isFile() && file.getName().endsWith(".jar");
			}
		};

		Collection<URL> urls = new HashSet<URL>();

		if(extendedclasspath != null && ! "".equals(extendedclasspath)) {
			for(String pathElement: extendedclasspath.split(";")) {
				logger.debug("Setting class directories to: " + pathElement);

				File path = new File(pathElement);

				if(path.isDirectory()) {
					for(File f: path.listFiles(filter)) {
						try {
							urls.add(f.toURI().toURL());
						} catch (MalformedURLException e) {
							logger.fatal("What is this library " + f);
						}
					}
				}
				else if(filter.accept(path)) {
					try {
						urls.add(path.toURI().toURL());
					} catch (MalformedURLException e) {
						logger.fatal("What is this library " + path);
					}
				}
			}
		}

		for(URL u: libspath) {
			urls.add(u);
		}

		URL[] arrayUrl = new URL[urls.size()];
		urls.toArray(arrayUrl);
		if(logger.isDebugEnabled())
			logger.debug("Internal class loader will look in:" + urls);
		return  URLClassLoader.newInstance(arrayUrl, getClass().getClassLoader());
	}

	private File prepareDir(File dir) {
		if( ! dir.exists()) {
			if(! autocreate) {
				logger.error(dir + " doesn't exists");
				return null;
			}
			if ( autocreate && !dir.mkdirs()) {
				logger.error(dir + " doesn't exists and can't be created");
				return null;
			}
		}
		else if( ! dir.isDirectory()) {
			logger.error(dir + " exists but is not a Directory");
			return null;
		}
		else if( ! dir.canWrite()) {
			logger.error(dir + " exists can not be written");
			return null;
		}
		return dir;
	}

	private File prepareDir(String path) {
		if(path == null || "".equals(path)) {
			return null;
		}
		File dir = new File(path);
		return prepareDir(dir);
	}

	public void update()
	{
		boolean nologgin = parseBoolean(getProperty("nologging", "false"));
		if(! nologgin) {
			for(String ls: new String[]{ "trace", "debug", "info", "error", "fatal", "warn"}) {
				Level l = Level.toLevel(ls);
				String param = getProperty("log." + ls, "");
				if(! "".equals(param)) {
					List<String> loggerList = Arrays.asList(param.split(","));
					loglevels.put(l, loggerList);
				}

			}
			loglevel = Level.toLevel(getProperty("loglevel", "info"));
			logfile = getProperty("logfile", "");

			//Let's configure the log fast
			try {
				jrds.JrdsLoggerConfiguration.configure(this);
			} catch (IOException e1) {
				logger.error("Unable to set log file to " + this.logfile + ": " + e1);
			}
		}
		legacymode = parseBoolean(getProperty("legacymode", "1"));

		//Directories configuration
		autocreate = parseBoolean(getProperty("autocreate", "false"));
		configdir = prepareDir(getProperty("configdir"));
		rrddir = prepareDir(getProperty("rrddir"));
		//Different place to find the tempdirectory
		tmpdir = prepareDir(getProperty("tmpdir"));
		if(tmpdir == null)
			tmpdir = prepareDir(System.getProperty("javax.servlet.context.tempdir"));
		if(tmpdir == null) {
			String tmpDirPath = System.getProperty("java.io.tmpdir");
			if(tmpDirPath != null || "".equals(tmpDirPath))
				tmpdir = prepareDir(new File(tmpDirPath, "jrds"));
		}

		step = parseInteger(getProperty("step", "300"));
		timeout = parseInteger(getProperty("timeout", "30"));
		collectorThreads = parseInteger(getProperty("collectorThreads", "1"));
		dbPoolSize = parseInteger(getProperty("dbPoolSize", "10")) + collectorThreads;
		syncPeriod = parseInteger(getProperty("syncPeriod", "-1"));
		String libspathString = getProperty("libspath", "");
		if(! "".equals(libspathString)) {
			for(String libName: libspathString.split(";")) {
				File lib = new File(libName);
				if(lib.isFile() || lib.isDirectory())
					try {
						libspath.add(lib.toURI().toURL());
					} catch (MalformedURLException e) {
						logger.fatal("What is this library " + lib);
					}
					else
						logger.error("Invalid lib path: "+ libName);
			}
		}
		extensionClassLoader = doClassLoader(getProperty("classpath", ""));

		rrdbackend = getProperty("rrdbackend", "NIO");

		Locale.setDefault(new Locale("POSIX"));
	}

	public File configdir;
	public File rrddir;
	public File tmpdir;
	public String urlpngroot;
	public String logfile;
	public int step;
	public int collectorThreads;
	public int dbPoolSize;
	public int syncPeriod;
	public final Set<URL> libspath = new HashSet<URL>();
	public ClassLoader extensionClassLoader;
	public final Map<Level, List<String>> loglevels = new HashMap<Level, List<String>>();
	public Level loglevel;
	public boolean legacymode;
	public boolean autocreate;
	public int timeout;
	public String rrdbackend;

}
