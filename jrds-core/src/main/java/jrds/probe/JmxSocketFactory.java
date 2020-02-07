package jrds.probe;

import java.io.IOException;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

import org.slf4j.event.Level;

import jrds.starter.SocketFactory;
import jrds.starter.Starter;

public class JmxSocketFactory extends Starter implements RMIClientSocketFactory {
    
    private static class RMIFastClientSocketFactory implements RMIClientSocketFactory {

        private final javax.net.SocketFactory factory;
        RMIFastClientSocketFactory(javax.net.SocketFactory factory) {
            this.factory = factory;
        }
        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return factory.createSocket(host, port);
        }
    }

    private RMIFastClientSocketFactory factory;
    
    @Deprecated
    public Socket createSocket(String host, int port) throws IOException {
        log(Level.DEBUG, "creating a RMI socket to %s:%d", host, port);
        return getLevel().find(SocketFactory.class).createSocket(host, port);
    }
    
    public synchronized RMIClientSocketFactory getFactory() {
        if (factory == null) {
            factory = new RMIFastClientSocketFactory(getLevel().find(SocketFactory.class).getFactory());
        }
        return factory;
    }

}
