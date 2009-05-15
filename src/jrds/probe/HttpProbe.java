package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.RdsHost;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 * 
 * A generic probe to collect an http service
 * default generic : 
 * port to provide a default port to collect
 * file to provide a specific file to collect
 * 
 * Implemation should implemenant the parseStream method
 *
 * TODO 
 */
public abstract class HttpProbe extends Probe implements UrlProbe {
	static final private Logger logger = Logger.getLogger(HttpProbe.class);
	protected static final String EMPTYHOST="";
	private URL url = null;
	private String host = EMPTYHOST;
	private int	   port = 0;
	private String file = null;
	private String label;

	public HttpProbe(URL url) {
		super();
		this.url = url;
	}
	
	public HttpProbe(Integer port, String file) {
		super();
		this.port = port;
		this.file = file;
	}

	public HttpProbe(Integer port) {
		super();
		this.port = port;
	}

	public HttpProbe(String file) {
		super();
		this.file = file;
	}

/*	public HttpProbe(List<Object> l) {
		List<Object> defaultArgs = getPd().getDefaultArgs();
		int i=0;
		for(Object defArg: defaultArgs ) {
			if(defArg != null && l.get(i) ==)
		}
		Object portObject = l.get(0);
		if(portObject != null && portObject instanceof Integer) {
			port = (Integer) portObject;
		}
		Object fileObject = l.get(1);
		if(fileObject != null && fileObject instanceof String) {
			file = (String) portObject;
		}
		StringBuilder urlString = new StringBuilder();
		
		Formatter f = new Formatter(urlString);
		f.format(format, args)
	}
*/
	public HttpProbe() {
		super();
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
	public Map getNewSampleValues() {
		Map<String, Number> vars = java.util.Collections.emptyMap();
		logger.debug("Getting " + getUrl());
		try {
			vars = parseStream(getUrl().openStream());
		} catch (IOException e) {
			logger.error("Unable to read url " + getUrl() + " because: " + e.getMessage());
		}
		return vars;
	}

	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		logger.trace("Set host to " + monitoredHost);
		host = monitoredHost.getName();
		try {
			if(url != null && EMPTYHOST.equals(getUrl().getHost()))
				setUrl(new URL("http", host, getUrl().getPort(), getUrl().getFile()));
		} catch (MalformedURLException e) {
			logger.error("URL " + "http://" + host + ":" + getUrl().getPort() + getUrl().getFile() + " is invalid");
		}
		logger.debug("Url to collect is " + getUrl());
	}

	/**
	 * @return Returns the url.
	 */
	public String getUrlAsString() {
		return getUrl().toString();
	}

	/**
	 * @return Returns the url.
	 */
	public URL getUrl() {
		if(url == null) {
			try {
				if(port == 0)
					port = Integer.parseInt(getPd().getSpecific("port"));
				if(file == null)
					file = getPd().getSpecific("file");

				url = new URL("http", host, port, file);
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
