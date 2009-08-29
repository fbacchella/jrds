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
		super.configure();
		this.port = port;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#setHost(jrds.RdsHost)
	 */
	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		try {
			remoteUrl = new URL("http", monitoredHost.getName(), port, "/");
			String kstatPath[] = getPd().getSpecific("kstat").split(":");
			String module = kstatPath[0];
			String instStr = kstatPath[1];
			String name = kstatPath[2];
			int inst = -1;
			inst = Integer.parseInt(instStr);
			ks = new Kstat(module, inst, name);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
		}
	}

	public  static void main(String[] args) throws MalformedURLException {
		ProbeDesc pd = new ProbeDesc();
		pd.addSpecific("kstat", "zfs:0:arcstats");
		KstatProbe probe = new KstatProbe(2002);
		probe.setPd(pd);
		probe.setHost(new RdsHost("ng263.prod"));
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
		Long uptime = (Long)remoteJk.getKstat("unix", 0, "system_misc").getData("boot_time");
		long now = System.currentTimeMillis() / 1000;
		return now - uptime.longValue() ;
	}

}
