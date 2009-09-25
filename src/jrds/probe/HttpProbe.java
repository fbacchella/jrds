package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.RdsHost;
import jrds.starter.Resolver;
import jrds.starter.Starter;

import org.apache.log4j.Logger;

/**

 * A generic probe to collect an HTTP service
 * default generic : 
 * port to provide a default port to collect
 * file to provide a specific file to collect
 * 
 * Implemention should implement the parseStream method
 *
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public abstract class HttpProbe extends Probe<String, Number> implements UrlProbe {
	static final private Logger logger = Logger.getLogger(HttpProbe.class);
	private URL url = null;
	private String host = null;
	private int	   port = 0;
	private String file = null;
	private String label;
	private List<Object> argslist = null;
	Starter resolver = null;

	public void configure(URL url) {
		this.url = url;
		finishConfigure();
	}

	public void configure(Integer port, String file) {
		this.port = port;
		this.file = file;
		finishConfigure();
	}

	public void configure(Integer port) {
		this.port = port;
		finishConfigure();
	}

	public void configure(String file) {
		this.file = file;
		finishConfigure();
	}

	public void configure(List<Object> argslist) {
		this.argslist = argslist;
		finishConfigure();
	}

	public void configure(URL url, List<Object> argslist) {
		this.url = url;
		this.argslist = argslist;
		finishConfigure();
	}

	public void configure(Integer port, String file, List<Object> argslist) {
		this.port = port;
		this.file = file;
		this.argslist = argslist;
		finishConfigure();
	}

	public void configure() {
		finishConfigure();
	}

	private void finishConfigure() {
		RdsHost monitoredHost = getHost();
		logger.trace("Set host to " + monitoredHost);
		host = monitoredHost.getDnsName();
		try {
			if(url != null)
				setUrl(new URL(getUrl().getProtocol(), host, getUrl().getPort(), getUrl().getFile()));
		} catch (MalformedURLException e) {
			logger.error("URL " + "http://" + host + ":" + getUrl().getPort() + getUrl().getFile() + " is invalid");
		}
		URL tempurl = getUrl();
		if("http".equals(tempurl.getProtocol())) {
			resolver = new Resolver(url.getHost()).register(getHost());
		}
		logger.debug("Url to collect is " + getUrl());
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#isCollectRunning()
	 */
	@Override
	public boolean isCollectRunning() {
		if (resolver != null && ! resolver.isStarted())
			return false;
		return super.isCollectRunning();
	}

	/**
	 * @param A stream collected from the http source
	 * @return a map of collected value
	 */
	protected abstract Map<String, Number> parseStream(InputStream stream);

	/**
	 * A utility method that transform the input stream to a List of lines
	 * @param stream
	 * @return
	 */
	public List<String> parseStreamToLines(InputStream stream) {
		List<String> lines = java.util.Collections.emptyList();
		logger.debug("Getting " + getUrl());
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			lines = new ArrayList<String>();
			String lastLine;
			while((lastLine = in.readLine()) != null)
				lines.add(lastLine);
			in.close();
		} catch (IOException e) {
			logger.error("Unable to read url " + getUrl() + " because: " + e.getMessage());
		}
		return lines;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map<String, Number> getNewSampleValues() {
		String hostName = getUrl().getHost();
		if(hostName != null) {
			Starter resolver = getStarters().find(Resolver.makeKey(hostName));
			if(resolver != null && ! resolver.isStarted()) {
				logger.trace("Resolver not started for " + getUrl().getHost());
				return Collections.emptyMap();
			}
		}
		Map<String, Number> vars = java.util.Collections.emptyMap();
		logger.debug("Getting " + getUrl());
		URLConnection cnx = null;
		try {
			cnx = getUrl().openConnection();
			cnx.setConnectTimeout(getTimeout() * 1000);
			cnx.setReadTimeout(getTimeout() * 1000);
			cnx.connect();
		} catch (IOException e) {
			logger.error("Unable to read url " + getUrl() + " because: " + e);
		}
		try {
			InputStream is = cnx.getInputStream();
			vars = parseStream(is);
			is.close();
		} catch (IOException e) {
			//Clean http connection error management
			//see http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
			try {
				byte[] buffer = new byte[4096];
				int respCode = ((HttpURLConnection)cnx).getResponseCode();
				logger.error("Unable to read url " + getUrl() + " because: " + e +", http error code: " + respCode);
				InputStream es = ((HttpURLConnection)cnx).getErrorStream();
				// read the response body
				while (es.read(buffer) > 0) {}
				// close the error stream
				es.close();
			} catch(IOException ex) {
				logger.error("Unable to recover from error in url " + getUrl() + " because: " + e);
			}
		}

		return vars;
	}

	/**
	 * @return Returns the url.
	 */
	public String getUrlAsString() {
		return getUrl().toString();
	}

	public int getPort() {
		return getUrl().getPort();
	}
	/**
	 * @return Returns the url.
	 */
	public URL getUrl() {
		if(url == null) {
			try {
				String portStr = null;
				if(port == 0) {
					portStr = getPd().getSpecific("port");
					if(portStr == null || "".equals(portStr)) {
						portStr = "80";
					}
				}
				if(file == null) {
					file = getPd().getSpecific("file");
					if(file == null || "".equals(file)) {
						file = "/";
					}
				}
				if(argslist != null) {
					try {
						String urlString = String.format("http://" + host + ":" + portStr + file, argslist.toArray());
						url = new URL(urlString);
					} catch (IllegalFormatConversionException e) {
						logger.error("Illegal format string: " + "http://" + host + ":" + portStr + file + ", args: " + argslist.size());
						return null;
					}
				}
				else {
					if(port == 0)
						port = jrds.Util.parseStringNumber(portStr, Integer.class, 80).intValue();
					url = new URL("http", host, port, file);
				}
			} catch (MalformedURLException e) {
				logger.error("URL " + "http://" + host + ":" + port + file + " is invalid");
			}
		}
		return url;
	}

	/**
	 * @param url The url to set.
	 */
	public void setUrl(URL url) {
		this.url = url;
	}

	@Override
	public String getSourceType() {
		return "HTTP";
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
