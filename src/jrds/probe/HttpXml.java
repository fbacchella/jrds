package jrds.probe;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.XmlProvider;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * @author Fabrice Bacchella 
 * @version $Revision: 407 $,  $Date: 2007-02-22 18:48:03 +0100 (jeu., 22 févr. 2007) $
 */
public class HttpXml extends HttpProbe {

	static final private Logger logger = Logger.getLogger(HttpXml.class);

	public XmlProvider xmlstarter = null;
	private List<Object> args = null;
	private Set<String> xpaths = null;
	private Map<String, String> collectKeys = null;

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
	public HttpXml(List<Object> args) {
		super(args);
		this.args = args;
	}
	public HttpXml(URL url, List<Object> args) {
		super(url);
		this.args = args;
	}
	public HttpXml(Integer port, String file, List<Object> args) {
		super(port, file);
		this.args = args;
	}

	public HttpXml() {
		super();
	}


	/* (non-Javadoc)
	 * @see jrds.Probe#setPd(jrds.ProbeDesc)
	 */
	@Override
	public void setPd(ProbeDesc pd) {
		super.setPd(pd);
		if(args != null) {
			xpaths = new HashSet<String>(getPd().getCollectStrings().size());
			collectKeys = new HashMap<String, String>(xpaths.size());
			for(Map.Entry<String, String> e:getPd().getCollectStrings().entrySet()) {
				String xpath = e.getKey();
				String dsName = e.getValue();
				String solved = String.format(xpath, args.toArray());
				xpaths.add(solved);
				collectKeys.put(solved, dsName);
			}
			//args will not used again, can be discarded
			args = null;
		}
		else {
			xpaths = getPd().getCollectStrings().keySet();
			collectKeys = getPd().getCollectStrings();
		}
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getCollectkeys()
	 */
	@Override
	public Map<String, String> getCollectkeys() {
		return collectKeys;
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
		xmlstarter.fileFromXpaths(d, xpaths, vars);
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
