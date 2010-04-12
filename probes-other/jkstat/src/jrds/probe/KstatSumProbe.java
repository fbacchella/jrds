package jrds.probe;

import java.util.HashMap;
import java.util.Map;

import jrds.ProbeConnected;
import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.api.KstatData;

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
			for(Map.Entry<String,KstatData> e: active.getMap().entrySet()) {
				KstatData data = e.getValue();
				String kname = e.getKey();
				if(! e.getValue().isNumeric())
					continue;
				if(! retValues.containsKey(kname)) {
					retValues.put(kname, (Number)data.getData());
					continue;
				}
				Number oldsum = retValues.get(kname);
				Number newvalue = (Number) data.getData();
				switch(data.getType()) {
				case KSTAT_DATA_DOUBLE:
				case KSTAT_DATA_UINT64:
					double sumdouble = oldsum.doubleValue() + newvalue.doubleValue();
					retValues.put(kname, sumdouble);
					break;
				case KSTAT_DATA_FLOAT: 
					double sumfloat = oldsum.floatValue() + newvalue.floatValue();
					retValues.put(kname, sumfloat);
					break;
				case KSTAT_DATA_INT32:
					double suminteger = oldsum.intValue() + newvalue.intValue();
					retValues.put(kname, suminteger);
					break;
				case KSTAT_DATA_INT64:
				case KSTAT_DATA_UINT32:
					double sumlong = oldsum.intValue() + newvalue.intValue();
					retValues.put(kname, sumlong);
					break;
				}
			}
		}
		return retValues;
	}

	public String getSourceType() {
		return "kstat";
	}

}
