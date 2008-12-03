package jrds.probe;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jrds.RdsHost;
import jrds.XmlProvider;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class HttpXml extends HttpProbe {

	static final private Logger logger = Logger.getLogger(HttpXml.class);

	public XmlProvider xmlstarter = null;

	public HttpXml(URL url) {
		super(url);
	}

	public HttpXml(Integer port, String file) {
		super(port, file);
	}

	public HttpXml(String file) {
		super(file);
	}

	public HttpXml(Integer port) {
		super(port);
	}

	public HttpXml() {
		super();
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#setHost(jrds.RdsHost)
	 */
	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		xmlstarter = new XmlProvider(monitoredHost);
		xmlstarter = (XmlProvider) xmlstarter.register(monitoredHost);
	}

	public Map<String, Number> dom2Map(Document d, Map<String, Number> variables) {
		return variables;
	}

	protected long findUptime(Document d) {
		String upTimePath = getPd().getSpecific("upTimePath");
		if(upTimePath == null) {
			logger.error("No xpath for the uptime for " + this);
			return 0;
		}
		return xmlstarter.findUptime(d, upTimePath);
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#parseStream(java.io.InputStream)
	 */
	@Override
	protected Map<String, Number> parseStream(InputStream stream) {
		Document d = xmlstarter.getDocument(stream);
		setUptime(findUptime(d));
		Map<String, Number> vars = new HashMap<String, Number>();
		xmlstarter.fileFromXpaths(d, getPd().getCollectStrings().keySet(), vars);
		logger.trace(vars);
		vars = dom2Map(d, vars);
		logger.trace(vars);
		return vars; 
	}

	@Override
	public String getSourceType() {
		return "HttpXml";
	}

}
