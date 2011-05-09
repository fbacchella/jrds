package jrds.probe;

import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
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
public class HttpXml extends HCHttpProbe {

	private Set<String> xpaths = null;
	private Map<String, String> collectKeys = null;

	public void configure(List<Object> args) {
		super.configure(args);
		finishConfig(args);
	}

    public void configure(String file, List<Object> args) {
        super.configure(file, args);
        finishConfig(args);
    }

    public void configure(Integer port, List<Object> args) {
		super.configure(port, args);
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
	    log(Level.TRACE, "Configuring collect xpath with %s", args);
		xpaths = new HashSet<String>(getPd().getCollectStrings().size());
		collectKeys = new HashMap<String, String>(xpaths.size());
		for(Map.Entry<String, String> e:getPd().getCollectStrings().entrySet()) {
			String xpath = e.getKey();
			String dsName = e.getValue();
			String solved = String.format(xpath, args.toArray());
			xpaths.add(solved);
			collectKeys.put(solved, dsName);
		}
        log(Level.TRACE, "collect xpath mapping %s", collectKeys);
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
		registerStarter(new XmlProvider());
	}

	public Map<String, Number> dom2Map(Document d, Map<String, Number> variables) {
		return variables;
	}

	protected long findUptime(XmlProvider xmlstarter, Document d) {
		String upTimePath = getPd().getSpecific("upTimePath");
		if(upTimePath != null) {
			return xmlstarter.findUptime(d, upTimePath);
		}
		else if(getPd().getSpecific("nouptime") != null) {
            return Long.MAX_VALUE;
        }
		else {
			String startTimePath = getPd().getSpecific("startTimePath");
			String currentTimePath = getPd().getSpecific("currentTimePath");
			String timePattern = getPd().getSpecific("timePattern");
			if(startTimePath != null && currentTimePath != null && timePattern !=null) {
				DateFormat df = new SimpleDateFormat(timePattern);
				return xmlstarter.findUptimeByDate(d, startTimePath, currentTimePath, df);
			}
			else {
				log(Level.ERROR, "No xpath for the uptime");
				return 0;
			}
		}
	}

	/* (non-Javadoc)
	 * @see jrds.probe.HttpProbe#parseStream(java.io.InputStream)
	 */
	@Override
	protected Map<String, Number> parseStream(InputStream stream) {
		XmlProvider xmlstarter  = find(XmlProvider.class);
		if(xmlstarter == null) {
			log(Level.ERROR, "XML Provider not found");
			return Collections.emptyMap();
		}

		Document d = xmlstarter.getDocument(stream);
		setUptime(findUptime(xmlstarter, d));
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
