/**
 * 
 */
package jrds.snmp;

import java.io.IOException;

import org.slf4j.event.Level;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.Snmp;
import org.snmp4j.fluent.SnmpBuilder;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import fr.jrds.snmpcodec.OIDFormatter;
import jrds.snmp.log.Slf4jLogFactory;
import jrds.starter.Starter;

public class MainStarter extends Starter {

    // Used to setup the log configuration of SNMP4J
    static {
        // Don't care about strict conformity
        SNMP4JSettings.setAllowSNMPv2InV1(true);
        org.snmp4j.log.LogFactory.setLogFactory(new Slf4jLogFactory());
    }

    static OIDFormatter formatter;

    public volatile Snmp snmp = null;

    public boolean start() {
        boolean started = false;
        try {
            SnmpBuilder snmpBuilder = new SnmpBuilder();
            snmp = snmpBuilder.v2c().build();
            DefaultTcpTransportMapping ttm = new DefaultTcpTransportMapping();
            ttm.setConnectionTimeout(getLevel().getTimeout() * 1000L);
            snmp.addTransportMapping(ttm);
            DefaultUdpTransportMapping utm = new DefaultUdpTransportMapping();
            utm.setSocketTimeout(getLevel().getTimeout() * 1000);
            snmp.addTransportMapping(utm);
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
}
