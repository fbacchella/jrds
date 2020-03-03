package jrds.probe.munin;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.event.Level;

import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.SocketFactory;
import lombok.Getter;
import lombok.Setter;

@ProbeBean({"port"})
public class MuninConnection extends Connection<MuninConnection.SocketChannels> {

    public final static class SocketChannels implements Closeable {
        public final PrintWriter out;
        public final BufferedReader in;
        public final Socket muninsSocket;
        SocketChannels(Socket muninSocket) throws IOException {
            this.muninsSocket = muninSocket;
            in = new BufferedReader(new InputStreamReader(muninSocket.getInputStream()));
            out = new PrintWriter(muninSocket.getOutputStream(), true);
        }
        @Override
        public void close() throws IOException {
            out.close();
            in.close();
            muninsSocket.close();
        }
    }
    static public final int DEFAULTMUNINPORT = 4949;

    private SocketChannels channel = null;

    @Getter @Setter
    private int port = DEFAULTMUNINPORT;

    public MuninConnection() {
    }

    public MuninConnection(Integer port) {
        this.port = port;
    }

    /**
     * @see jrds.starter.Connection#getConnection()
     */
    @Override
    public SocketChannels getConnection() {
        return channel;
    }

    /**
     * @see jrds.starter.Connection#setUptime()
     */
    @Override
    public long setUptime() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean startConnection() {
        SocketFactory ss = getLevel().find(SocketFactory.class);
        try {
            channel = new SocketChannels(ss.getFactory().createSocket(getHostName(), port));
        } catch (IOException e) {
            log(Level.ERROR, e, "Connection error", e);
            return false;
        }
        return true;
    }

    @Override
    public void stopConnection() {
        try {
            channel.out.println("quit");
            InputStream is = channel.muninsSocket.getInputStream();
            int available = is.available();
            while(available > 0) {
                is.skip(available);
                available = is.available();
            }
            channel.close();
            channel = null;
        } catch (IOException e) {
            log(Level.WARN, e, "Connection error during close", e);
        }
    }

}
