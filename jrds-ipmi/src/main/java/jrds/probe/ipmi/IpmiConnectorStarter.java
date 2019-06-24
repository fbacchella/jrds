package jrds.probe.ipmi;

import java.io.IOException;

import org.slf4j.event.Level;

import com.veraxsystems.vxipmi.api.sync.IpmiConnector;

import jrds.starter.Starter;

public class IpmiConnectorStarter extends Starter {

    volatile IpmiConnector connector = null;

    @Override
    public boolean start() {
        try {
            connector = new IpmiConnector(0);
            return true;
        } catch (IOException e) {
            log(Level.ERROR, "failed to start IPMI: %s", e);
            return false;
        }
    }

    @Override
    public void stop() {
        connector.tearDown();
        connector = null;
    }

}
