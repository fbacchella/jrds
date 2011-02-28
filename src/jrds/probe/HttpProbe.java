package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
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

import org.apache.log4j.Level;

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
	private URL url = null;
	private String host = null;
	private int	   port = 0;
	private String file = null;
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

	public void configure(Integer port, List<Object> argslist) {
		this.port = port;
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
		log(Level.TRACE, "Set host to %s", monitoredHost);
		host = monitoredHost.getDnsName();
		try {
			if(url != null)
				setUrl(new URL(getUrl().getProtocol(), host, getUrl().getPort(), getUrl().getFile()));
		} catch (MalformedURLException e) {
			log(Level.ERROR, e, "URL " + "http://%d:%s%s is invalid", host, getUrl().getPort(), getUrl().getFile());
		}
		URL tempurl = getUrl();
		if("http".equals(tempurl.getProtocol())) {
			resolver = getHost().registerStarter(new Resolver(url.getHost()));
		}
		log(Level.DEBUG, "URL to collect is %s", getUrl());
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#isCollectRunning()
	 */
	@Override
	public boolean isCollectRunning() {
		if (resolver == null || ! resolver.isStarted())
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
		log(Level.DEBUG, "Getting %s", getUrl());
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			lines = new ArrayList<String>();
			String lastLine;
			while((lastLine = in.readLine()) != null)
				lines.add(lastLine);
			in.close();
		} catch (IOException e) {
			log(Level.ERROR, e, "Unable to read url %s because: %s", getUrl(), e.getMessage());
		}
		return lines;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map<String, Number> getNewSampleValues() {
		log(Level.DEBUG, "Getting %s", getUrl());
		URLConnection cnx = null;
		try {
			cnx = getUrl().openConnection();
			cnx.setConnectTimeout(getTimeout() * 1000);
			cnx.setReadTimeout(getTimeout() * 1000);
			cnx.connect();
		} catch (IOException e) {
			log(Level.ERROR, e, "Connection to %s failed: %s", getUrl(), e.getMessage());
			return Collections.emptyMap();
		}
		try {
			InputStream is = cnx.getInputStream();
			Map<String, Number> vars = parseStream(is);
			is.close();
			return vars;
		} catch(ConnectException e) {
			log(Level.ERROR, e, "Connection refused to %s", getUrl());
		} catch (IOException e) {
			//Clean http connection error management
			//see http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
			try {
				byte[] buffer = new byte[4096];
				int respCode = ((HttpURLConnection)cnx).getResponseCode();
				log(Level.ERROR, e, "Unable to read url %s because: %s, http error code: %d", getUrl(), e.getMessage(), respCode);
				InputStream es = ((HttpURLConnection)cnx).getErrorStream();
				// read the response body
				while (es.read(buffer) > 0) {}
				// close the error stream
				es.close();
			} catch(IOException ex) {
				log(Level.ERROR, ex, "Unable to recover from error in url %s because %s", getUrl(), ex.getMessage());
			}
		}

		return Collections.emptyMap();
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
				if(port == 0) {
					String portStr = getPd().getSpecific("port");
					if(portStr != null && ! "".equals(portStr) && argslist != null)
						portStr = String.format(portStr, argslist.toArray());
					port = jrds.Util.parseStringNumber(portStr, Integer.class, 80).intValue();
				}
				if(file == null) {
					file = getPd().getSpecific("file");
					if(file == null || "".equals(file)) {
						file = "/";
					}
				}
				if(argslist != null) {
					try {
						String urlString = String.format("http://" + host + ":" + port + file, argslist.toArray());
						url = new URL(urlString);
					} catch (IllegalFormatConversionException e) {
						log(Level.ERROR, "Illegal format string: http://%s:%d%s, args %d", host, port, file, argslist.size());
						return null;
					}
				}
				else {
					url = new URL("http", host, port, file);
				}
			} catch (MalformedURLException e) {
				log(Level.ERROR, e, "URL http://%s:%s%s is invalid",host , port, file);
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
}
