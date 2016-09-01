package jrds.probe.jmx;

import java.io.IOException;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

import org.apache.log4j.Level;

import jrds.starter.SocketFactory;
import jrds.starter.Starter;

public class JmxSocketFactory extends Starter implements RMIClientSocketFactory {

    public Socket createSocket(String host, int port) throws IOException {
        log(Level.DEBUG, "creating a RMI socket to %s:%d", host, port);
        return getLevel().find(SocketFactory.class).createSocket(host, port);
    }

}
