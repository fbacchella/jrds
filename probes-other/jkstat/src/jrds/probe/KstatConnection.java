package jrds.probe;

import java.net.MalformedURLException;
import java.net.URL;

import uk.co.petertribble.jkstat.api.JKstat;
import uk.co.petertribble.jkstat.api.Kstat;
import uk.co.petertribble.jkstat.client.RemoteJKstat;
import jrds.starter.Connection;

public class KstatConnection extends Connection {
	int port;
	JKstat remoteJk = null;

	public KstatConnection(Integer port) {
		this.port = port;
	}

	@Override
	public Object getConnection() {
		return remoteJk;
	}

	@Override
	public long setUptime() {
		Kstat ks = remoteJk.getKstat("unix", 0, "system_misc");
		if(ks == null) {
			return 0;
		}
		Long uptime = (Long)ks.getData("boot_time");
		long now = System.currentTimeMillis() / 1000;
		return now - uptime.longValue() ;
	}

	@Override
	public boolean startConnection() {
		try {
			String hostName = getHostName();
			URL remoteUrl = new URL("http", hostName, port, "/");
			 remoteJk = new RemoteJKstat(remoteUrl.toString());
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void stopConnection() {
		remoteJk = null;
	}

}
