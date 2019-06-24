package jrds.probe.munin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.event.Level;

import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.SocketFactory;

@ProbeBean({"port"})
public class MuninConnection extends Connection<MuninConnection.SocketChannels> {
    public final static class SocketChannels {
        PrintWriter out;
        BufferedReader in;
        Socket muninsSocket;
    }
    static final int DEFAULTMUNINPORT = 4949;

    private SocketChannels channel = null;
    private int port = DEFAULTMUNINPORT;

    public MuninConnection() {
        super();
    }

    public MuninConnection(Integer port) {
        super();
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
        channel = new SocketChannels();
        try {
            channel.muninsSocket = ss.createSocket(getHostName(), port);
            channel.out = new PrintWriter(channel.muninsSocket.getOutputStream(), true);
            channel.in = new BufferedReader(new InputStreamReader(channel.muninsSocket.getInputStream()));
        } catch (IOException e) {
            log(Level.ERROR, e, "Connection error", e.getMessage());
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
            channel.out.close();
            channel.in.close();
            channel.muninsSocket.close();
        } catch (IOException e) {
            log(Level.WARN, e, "Connection error during close", e.getMessage());
        }
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

}
