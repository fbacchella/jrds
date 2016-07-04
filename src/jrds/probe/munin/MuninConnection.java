package jrds.probe.munin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.SocketFactory;

import org.apache.log4j.Level;

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

    /* (non-Javadoc)
     * @see jrds.starter.Connection#getConnection()
     */
    @Override
    public SocketChannels getConnection() {
        return channel;
    }

    /* (non-Javadoc)
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
            log(Level.ERROR, e, "Connection error", e);
            return false;
        }
        return true;
    }

    @Override
    public void stopConnection() {
        try {
            channel.out.println("quit");
            int avalaible = channel.muninsSocket.getInputStream().available();
            while(avalaible > 0) {
                channel.muninsSocket.getInputStream().read(new byte[avalaible]);
                avalaible = channel.muninsSocket.getInputStream().available();
            }
            channel.muninsSocket.close();
        } catch (IOException e) {
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
