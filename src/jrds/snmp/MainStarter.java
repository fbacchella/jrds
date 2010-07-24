/**
 * 
 */
package jrds.snmp;

import java.io.IOException;

import jrds.starter.Starter;

import org.apache.log4j.Level;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class MainStarter extends Starter {
	public boolean start() {
		boolean started = false;
		try {
			SnmpStarter.snmp = new Snmp(new DefaultUdpTransportMapping());
			SnmpStarter.snmp.listen();
			started = true;
		} catch (IOException e) {
			log(Level.ERROR, e, "SNMP UDP Transport Mapping not started: %s", e);
			SnmpStarter.snmp = null;
		}
		return started;
	}

	public void stop() {
		try {
			SnmpStarter.snmp.close();
		} catch (IOException e) {
			log(Level.ERROR, e, "IO error while stop SNMP UDP Transport Mapping: %s", e);
		}
		SnmpStarter.snmp = null;
	}

	@Override
	public String toString() {
		return "SNMP root";
	}
}