package jrds.probe;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Map;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.message.Message;

import org.apache.log4j.Level;

import com.sun.jmx.remote.socket.SocketConnection;

import jrds.starter.SocketFactory;
import jrds.starter.Starter;

public class JmxSocketFactory extends Starter implements RMIClientSocketFactory {
    
    private final class JrdsSocketConnection extends SocketConnection {

        public JrdsSocketConnection(Socket socket) throws IOException {
            super(socket);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void connect(Map env) throws IOException {
            try {
                super.connect(env);
            } catch (EOFException e) {
                // don't log this one
                throw e;
            } catch (Exception e) {
                log(Level.ERROR, e, "failed to read message: %s", e);
                throw e;
            }
        }

        @Override
        public void writeMessage(Message msg) throws IOException {
            try {
                super.writeMessage(msg);
            } catch (EOFException e) {
                // don't log this one
                throw e;
            } catch (Exception e) {
                log(Level.ERROR, e, "failed to read message: %s", e);
                throw e;
            }
        }

        @Override
        public Message readMessage()
                throws IOException, ClassNotFoundException {
            try {
                return super.readMessage();
            } catch (EOFException e) {
                // don't log this one
                throw e;
            } catch (Exception e) {
                log(Level.ERROR, e, "failed to read message: %s", e);
                throw e;
            }
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
        SocketFactory sf = getLevel().find(SocketFactory.class); 
        Socket s = sf.createSocket(host, port);
        return new JrdsSocketConnection(s);
    }

}
