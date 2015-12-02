package jrds.probe;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Map;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.message.Message;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.jmx.remote.socket.SocketConnection;

import jrds.starter.SocketFactory;
import jrds.starter.Starter;

public class JmxSocketFactory extends Starter implements RMIClientSocketFactory {
    
    private static final Logger sclogger = Logger.getLogger("jrds.probe.JmxSocketFactory.JrdsSocketConnection");

    private final class JrdsSocketConnection extends SocketConnection {

        private final String host;
        private final int port;
        
        public JrdsSocketConnection(String host, int port) throws IOException {
            super(getLevel().find(SocketFactory.class).createSocket(host, port));
            this.host = host;
            this.port = port;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void connect(Map env) throws IOException {
            try {
                dolog(Level.TRACE, "connect");
                super.connect(env);
            } catch (EOFException e) {
                // don't log this one
                throw e;
            } catch (Exception e) {
                dolog(Level.ERROR, e, "failed to connect: %s", e);
                throw e;
            }
        }

        @Override
        public void writeMessage(Message msg) throws IOException {
            try {
                dolog(Level.TRACE, "writeMessage");
                super.writeMessage(msg);
            } catch (EOFException e) {
                // don't log this one
                throw e;
            } catch (Exception e) {
                dolog(Level.ERROR, e, "failed to write message: %s", e);
                throw e;
            }
        }

        @Override
        public Message readMessage()
                throws IOException, ClassNotFoundException {
            try {
                dolog(Level.TRACE, "readMessage");
                return super.readMessage();
            } catch (EOFException e) {
                // don't log this one
                throw e;
            } catch (Exception e) {
                dolog(Level.ERROR, e, "failed to read message: %s", e);
                throw e;
            }
        }
        
        private void dolog(Level l, Throwable e, String format, Object... elements) {
            jrds.Util.log(this, sclogger, l, e, "socket " + host + ":" + port + ": " + format, elements);            
        }

        private void dolog(Level l, String format, Object... elements) {
            jrds.Util.log(this, sclogger, l, null, "socket " + host + ":" + port + ": " + format, elements);            
        }

    };

    public Socket createSocket(String host, int port) throws IOException {
        log(Level.DEBUG, "creating a RMI socket to %s:%d", host, port);
        return getLevel().find(SocketFactory.class).createSocket(host, port);
    }
    
    public SocketConnection createSocketConnection(JMXServiceURL url) throws IOException {
        log(Level.DEBUG, "creating a JMXMP socket to %s", url);
        String host = url.getHost();
        int port = url.getPort();
        return new JrdsSocketConnection(host, port);
    }

}
