package jrds.probe;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.ConnectedProbe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.starter.Connection;

import org.apache.log4j.Logger;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.api.KstatData;

public class KstatProbe extends jrds.Probe  implements ConnectedProbe {
	static final private Logger logger = Logger.getLogger(KstatProbe.class);

	Kstat ks;
	private String connectionName = KstatConnection.class.getName();

	protected Boolean setup(String module, int instance,String name) {
		ks = new Kstat(module, instance, name);
		return true;
	}
	
	public Boolean configure() {
		String module = getPd().getSpecific("module");
		String name = getPd().getSpecific("name");
		return setup(module, 0, name);
	}

	public  static void main(String[] args) throws MalformedURLException {
		ProbeDesc pd = new ProbeDesc();
		Connection cnx = new KstatConnection(3000);
		pd.addSpecific("module", "${device}");
		pd.addSpecific("name", "statistics");
		pd.addSpecific("index", "${device}${instance}");
		KstatProbeIndexed probe = new KstatProbeIndexed();
		probe.setPd(pd);
		probe.setHost(new RdsHost("toto","10.0.0.127"));
		probe.configure("e1000g", 0);
		cnx.register(probe);
		probe.getStarters().startCollect();
		probe.getUptime();
		Map<?, ?> m =  probe.getNewSampleValues();
		for(Object key: m.keySet() ) {
			String keyName = key.toString();
			System.out.println("<ds>");
			System.out.println("    <dsName>" + keyName + "</dsName>");
			System.out.println("    <dsType></dsType>");
			if(keyName.length() > 20) {
				System.out.println("    <collect>" + keyName + "</collect>");
			}
			System.out.println("</ds>");
		}
		System.out.println(probe.getNewSampleValues());
	}

	public Map getNewSampleValues() {
		KstatConnection cnx = (KstatConnection) getStarters().find(connectionName);
		if(cnx == null) {
			logger.warn("No connection found for " + this + " with name " + getConnectionName());
		}
		if( !cnx.isStarted()) {
			return null;
		}
		//Uptime is collected only once, by the connexion
		setUptime(cnx.getUptime());

		JKstat remoteJk = (JKstat) cnx.getConnection();
		Kstat active  = remoteJk.getKstat(ks);
		if(active == null) {
			return Collections.emptyMap();
		}
		Map<String, Number> retValues = new HashMap<String,Number>();
		for(Map.Entry<String, KstatData> e: active.getMap().entrySet()) {
			String name = e.getKey();
			KstatData v = e.getValue();
			if(v.isNumeric()) {
				retValues.put(name, (Number)v.getData());
			}
		}
		return retValues;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	@Override
	public Map<?, Number> filterValues(Map valuesList) {
		Map<String, Number> retValues = new HashMap<String, Number>(getPd().getCollectStrings().size());
		for(String stat: getPd().getCollectStrings().values()) {
			Number val = (Number)valuesList.get(stat);
			if(val != null) {
				retValues.put(stat, val);
			}
		}
		return retValues;
	}

	public String getSourceType() {
		return "kstat";
	}

	public String getConnectionName() {
		return connectionName;
	}
	
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}
}
