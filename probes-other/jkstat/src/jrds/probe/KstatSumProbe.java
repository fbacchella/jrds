package jrds.probe;

import java.util.HashMap;
import java.util.Map;

import jrds.objects.probe.ProbeConnected;
import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;

public class KstatSumProbe extends ProbeConnected<String, Number, KstatConnection> {
	private String module;
	private String name;

	public KstatSumProbe() {
		super(KstatConnection.class.getName());
	}

	public Boolean configure() {
		module = getPd().getSpecific("module");
		name = getPd().getSpecific("name");
		return true;
	}

	public Map<String,Number> getNewSampleValuesConnected(KstatConnection cnx) {
		JKstat remoteJk = cnx.getConnection();
		int instance = 0;
		Kstat active = null;
		Map<String, Number> retValues = new HashMap<String, Number>();
		while( (active  = remoteJk.getKstat(String.format(module, instance), instance, String.format(name, instance))) != null) {
			instance++;
			for(String kname: active.statistics()) {
				if(! active.isNumeric(kname))
					continue;
                double newvalue = ((Number) active.getData(kname)).doubleValue();
				if(! retValues.containsKey(kname)) {
					retValues.put(kname, newvalue);
					continue;
				}
				double oldsum = retValues.get(kname).doubleValue();
                retValues.put(kname, oldsum + newvalue);
			}
		}
		return retValues;
	}

	public String getSourceType() {
		return "kstat";
	}

}
