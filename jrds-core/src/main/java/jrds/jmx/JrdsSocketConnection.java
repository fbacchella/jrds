package jrds.jmx;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.sun.jmx.remote.socket.SocketConnection;

import jrds.starter.SocketFactory;

public class JrdsSocketConnection extends SocketConnection {

    private static final Logger logger = LoggerFactory.getLogger(JrdsSocketConnection.class);

    private final String host;
    private final int port;

    /**
     * Used to create a SocketConnection But it wraps it in a generic object as
     * jmpxmp implementation is optionnal So it allows to use it in generic
     * class that don't reference any classes from jmxmp.jar
     * 
     * @param url The url to the jmxmp agent
     * @param sf a custom socket factory
     * @return a com.sun.jmx.remote.socket.SocketConnection to a jmpxmp agent
     * @throws IOException
     */
    public static Object create(JMXServiceURL url, SocketFactory sf) throws IOException {
        jrds.Util.log(null, logger, Level.DEBUG, null, "creating a JMXMP socket to %s", url);
        String host = url.getHost();
        int port = url.getPort();
        return new JrdsSocketConnection(host, port, sf);
    }

    public JrdsSocketConnection(String host, int port, SocketFactory sf) throws IOException {
        super(sf.createSocket(host, port));
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
    public Message readMessage() throws IOException, ClassNotFoundException {
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
        jrds.Util.log(this, logger, l, e, "socket " + host + ":" + port + ": " + format, elements);
    }

    private void dolog(Level l, String format, Object... elements) {
        jrds.Util.log(this, logger, l, null, "socket " + host + ":" + port + ": " + format, elements);
    }

}
