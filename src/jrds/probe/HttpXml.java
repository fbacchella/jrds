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
import jrds.starter.XmlProvider;

import org.apache.log4j.Level;
import org.w3c.dom.Document;

/**
 * @author Fabrice Bacchella 
 * @version $Revision: 407 $,  $Date: 2007-02-22 18:48:03 +0100 (jeu., 22 févr. 2007) $
 */
public class HttpXml extends HttpProbe {

	public XmlProvider xmlstarter = null;
	private Set<String> xpaths = null;
	private Map<String, String> collectKeys = null;

	public void configure(List<Object> args) {
		super.configure(args);
		finishConfig(args);
	}
	
	public void configure(URL url, List<Object> args) {
		super.configure(url, args);
		finishConfig(args);
	}

	public void configure(Integer port, String file, List<Object> args) {
		super.configure(port, file, args);
		finishConfig(args);
	}

	private void finishConfig(List<Object> args) {
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
	
	/* (non-Javadoc)
	 * @see jrds.Probe#setPd(jrds.ProbeDesc)
	 */
	@Override
	public void setPd(ProbeDesc pd) {
		super.setPd(pd);
		xpaths = getPd().getCollectStrings().keySet();
		collectKeys = getPd().getCollectStrings();
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getCollectkeys()
	 */
	@Override
	public Map<String, String> getCollectMapping() {
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
			log(Level.ERROR, "No xpath for the uptime");
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
		log(Level.TRACE, "%s", vars);
		vars = dom2Map(d, vars);
		log(Level.TRACE, "%s", vars);
		return vars; 
	}

	@Override
	public String getSourceType() {
		return "HttpXml";
	}


}
