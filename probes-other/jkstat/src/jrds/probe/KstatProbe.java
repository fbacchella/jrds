package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.api.KstatData;
import uk.co.petertribble.jkstat.client.RemoteJKstat;

public class KstatProbe extends jrds.Probe {
	int port;
	URL remoteUrl;
	Kstat ks;

	public void configure(Integer port) {
		this.port = port;
		try {
			remoteUrl = new URL("http",getHost().getName(), port, "/");
			String kstatPath[] = getPd().getSpecific("kstat").split(":");
			String module = kstatPath[0];
			String instStr = kstatPath[1];
			String name = kstatPath[2];
			int inst = jrds.Util.parseStringNumber(instStr, Integer.class, -1).intValue();
			ks = new Kstat(module, inst, name);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public  static void main(String[] args) throws MalformedURLException {
		ProbeDesc pd = new ProbeDesc();
		pd.addSpecific("kstat", "zfs:0:arcstats");
		KstatProbe probe = new KstatProbe();
		probe.setPd(pd);
		probe.setHost(new RdsHost("10.0.0.127"));
		probe.configure(7777);
		probe.getUptime();
		System.out.println(probe.getNewSampleValues());
	}

	public Map getNewSampleValues() {
		JKstat remoteJk = new RemoteJKstat(remoteUrl.toString());
		Kstat active  = remoteJk.getKstat(ks);
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
	
	/* (non-Javadoc)
	 * @see jrds.Probe#getUptime()
	 */
	public long getUptime() {
		JKstat remoteJk = new RemoteJKstat(remoteUrl.toString());
		Kstat ks = remoteJk.getKstat("unix", 0, "system_misc");
		if(ks == null)
			return 0;
		Long uptime = (Long)ks.getData("boot_time");
		long now = System.currentTimeMillis() / 1000;
		return now - uptime.longValue() ;
	}

}
