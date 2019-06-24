package jrds.probe;

import java.io.IOException;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

import org.slf4j.event.Level;

import jrds.starter.SocketFactory;
import jrds.starter.Starter;

public class JmxSocketFactory extends Starter implements RMIClientSocketFactory {

    public Socket createSocket(String host, int port) throws IOException {
        log(Level.DEBUG, "creating a RMI socket to %s:%d", host, port);
        return getLevel().find(SocketFactory.class).createSocket(host, port);
    }

}
