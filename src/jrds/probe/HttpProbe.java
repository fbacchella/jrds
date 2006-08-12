package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.Probe;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class HttpProbe extends Probe  implements IndexedProbe {
	static final private Logger logger = Logger.getLogger(HttpResponseTime.class);
	private URL url;

	/**
	 * @param monitoredHost
	 */
	public HttpProbe(URL url) {
		super();
		this.url = url;
	}
	
	protected abstract Map parseLines(List<String> lines);
	/* (non-Javadoc)
	 * @see com.aol.jrds.Probe#getNewSampleValues()
	 */
	public Map getNewSampleValues() {
		Map vars = null;
		BufferedReader  in;
		try {
			in = new BufferedReader(new InputStreamReader(url.openStream()));
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
}
