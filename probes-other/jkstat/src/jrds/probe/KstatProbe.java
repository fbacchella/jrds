package jrds.probe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.api.KstatData;

public class KstatProbe extends jrds.ProbeConnected<String, KstatData, KstatConnection> {

	private String module;
	private int instance;
	private String name;
	
	public KstatProbe() {
		super(KstatConnection.class.getName());
	}

	protected Boolean setup(String module, int instance, String name) {
		this.module = module;
		this.instance = instance;
		this.name = name;
		return true;
	}
	
	public Boolean configure() {
		String module = getPd().getSpecific("module");
		String name = getPd().getSpecific("name");
		return setup(module, 0, name) && super.configure();
	}

	public Map<String,KstatData> getNewSampleValuesConnected(KstatConnection cnx) {
		JKstat remoteJk = cnx.getConnection();
		Kstat active  = remoteJk.getKstat(module, instance, name);
		if(active == null) {
			return Collections.emptyMap();
		}
		return active.getMap();
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	@Override
	public Map<String, Number> filterValues(Map<String, KstatData> valuesList) {
		Map<String, Number> retValues = new HashMap<String, Number>(getPd().getCollectStrings().size());
		for(String stat: getPd().getCollectStrings().values()) {
			KstatData val = valuesList.get(stat);
			if(val != null && val.isNumeric()) {
				retValues.put(stat, (Number)val.getData());
			}
		}
		return retValues;
	}

	public String getSourceType() {
		return "kstat";
	}
}
