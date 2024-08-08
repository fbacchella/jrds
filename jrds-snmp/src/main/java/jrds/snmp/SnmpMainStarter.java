/**
 * 
 */
package jrds.snmp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.slf4j.event.Level;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;

import jrds.starter.Starter;

public class SnmpMainStarter extends Starter {

    private static class CustomUdpTransportMapping extends DefaultUdpTransportMapping {
        private final int timeout;
        private final int bufferSize;
        public CustomUdpTransportMapping(int timeout, int bufferSize) throws SocketException {
            this.timeout = timeout;
            this.bufferSize = bufferSize;
        }
        @Override
        protected synchronized DatagramSocket ensureSocket() throws SocketException {
            DatagramSocket ds = super.ensureSocket();
            if (timeout > 0) {
                ds.setSoTimeout(timeout);
            }
            if (bufferSize > 0) {
                ds.setReceiveBufferSize(bufferSize);
            }
            return ds;
        }
    }

    public volatile Snmp snmp = null;

    public boolean start() {
        boolean started = false;
        try {
            MessageDispatcher md = new MultiThreadedMessageDispatcher(new VirtualThreadWorkerPoll(), new MessageDispatcherImpl());
            md.addMessageProcessingModel(new MPv1());
            md.addMessageProcessingModel(new MPv2c());
            DefaultTcpTransportMapping ttm = new DefaultTcpTransportMapping();
            ttm.setServerEnabled(false);
            ttm.addTransportListener(md);
            ttm.setConnectionTimeout(getLevel().getTimeout() * 1000L);
            md.addTransportMapping(ttm);
            // Don't use UdpTransportMapping.setSocketTimeout(), it introduce long shutdown time
            DefaultUdpTransportMapping utm = new CustomUdpTransportMapping(getLevel().getTimeout(), -1);
            md.addTransportMapping(utm);
            utm.addTransportListener(md);
            snmp = new Snmp(md);
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
