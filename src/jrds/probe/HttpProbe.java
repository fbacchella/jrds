package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.RdsHost;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class HttpProbe extends Probe  implements IndexedProbe {
	static final private Logger logger = Logger.getLogger(HttpProbe.class);
	protected static final String EMPTYHOST="";
	private URL url;
	private String label;

	public HttpProbe(URL url) {
		super();
		this.url = url;
	}

	protected abstract Map parseLines(List<String> lines);
	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map vars = java.util.Collections.emptyMap();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			List<String> lines = new ArrayList<String>();
			String lastLine;
			while((lastLine = in.readLine()) != null)
				lines.add(lastLine);
			in.close();
			vars = parseLines(lines);
		} catch (IOException e) {
			logger.error("Unable to read url " + url + " because: " + e.getMessage());
		}
		return vars;
	}

	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		try {
			if(EMPTYHOST.equals(getUrl().getHost()))
				setUrl(new URL("http", monitoredHost.getName(), getUrl().getPort(), getUrl().getFile()));
		} catch (MalformedURLException e) {
		}
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
		return url;
	}

	/**
	 * @param url The url to set.
	 */
	public void setUrl(URL url) {
		this.url = url;
	}
	/**
	 * @see jrds.probe.IndexedProbe#getIndexName()
	 */
	public String getIndexName() {
		return url.toString();
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
