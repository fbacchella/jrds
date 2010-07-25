/**
 * 
 */
package jrds.snmp;

import java.io.IOException;

import jrds.JrdsLoggerConfiguration;
import jrds.starter.Starter;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.snmp4j.Snmp;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class MainStarter extends Starter {
	//Used to setup the log configuration of SNMP4J
	static {
		org.snmp4j.log.LogFactory.setLogFactory(new Log4jLogFactory());
		//If not already configured, we filter it
		Logger snmpLogger = LogManager.getLoggerRepository().exists("org.snmp4j");
		if(snmpLogger != null) {
			snmpLogger.setLevel(Level.ERROR);
			JrdsLoggerConfiguration.joinAppender("org.snmp4j");
		}
	}
	public volatile Snmp snmp = null;

	public boolean start() {
		boolean started = false;
		try {
			snmp = new Snmp(new DefaultUdpTransportMapping());
			snmp.listen();
			started = true;
		} catch (IOException e) {
			log(Level.ERROR, e, "SNMP UDP Transport Mapping not started: %s", e);
			snmp = null;
		}
		return started;
	}

	public void stop() {
		try {
			snmp.close();
		} catch (IOException e) {
			log(Level.ERROR, e, "IO error while stop SNMP UDP Transport Mapping: %s", e);
		}
		snmp = null;
	}
//
//	@Override
//	public String toString() {
//		return "SNMP root";
//	}
}