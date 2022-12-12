package jrds.probe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.event.Level;

import jrds.Probe;
import jrds.factories.ProbeBean;
import jrds.factories.ProbeMeta;
import jrds.starter.Resolver;
import jrds.starter.Starter;

@ProbeBean({ "port" })
@ProbeMeta(timerStarter=Ntp.NtpClientStarter.class)
public class Ntp extends Probe<String, Number> {

    public static class NtpClientStarter extends Starter {
        private ThreadLocal<NTPUDPClient> clientProvider;
        @Override
        public boolean start() {
            clientProvider = ThreadLocal.withInitial(NTPUDPClient::new);
            return super.start();
        }

        @Override
        public void stop() {
            clientProvider = null;
            super.stop();
        }

        public NTPUDPClient getUdpClient() {
            return clientProvider.get();
        }
    }

    int port = NTPUDPClient.DEFAULT_PORT;

    public Boolean configure() {
        return true;
    }

    public Boolean configure(Integer port) {
        this.port = port;
        return true;
    }

    @Override
    public Map<String, Number> getNewSampleValues() {
        Resolver resolv = find(Resolver.class);
        if(!resolv.isStarted()) {
            return null;
        } else {
            try {
                NTPUDPClient client = find(NtpClientStarter.class).getUdpClient();
                client.setDefaultTimeout(this.getTimeout() * 1000);
                TimeInfo ti = client.getTime(resolv.getInetAddress(), port);
                ti.computeDetails();
                NtpV3Packet pkct = ti.getMessage();
                Map<String, Number> retValues = new HashMap<>(4);
                retValues.put("RootDelay", pkct.getRootDelayInMillisDouble());
                retValues.put("RootDispersion", pkct.getRootDispersionInMillisDouble());
                retValues.put("Offset", ti.getOffset());
                retValues.put("Delay", ti.getDelay());
                return retValues;
            } catch (IOException e) {
                log(Level.ERROR, e, "NTP IO exception %s", e);
                return null;
            }
        }
    }

    @Override
    public String getSourceType() {
        return "NTP";
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#getUptime()
     */
    @Override
    public long getUptime() {
        return Long.MAX_VALUE;
    }

    /**
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Integer port) {
        this.port = port;
    }

}
