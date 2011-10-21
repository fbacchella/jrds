package jrds.probe.munin;

import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Level;

import jrds.starter.Connection;
import jrds.starter.SocketFactory;

public class MuninConnection extends Connection<Socket> {
    private final int DEFAULTMUNINPORT = 4949;
    private Socket muninsSocket = null;
    private final int port;

    public MuninConnection() {
        super();
        port = DEFAULTMUNINPORT;
    }

    public MuninConnection(Integer port) {
        super();
        this.port = port;
    }

    @Override
    public Socket getConnection() {
        return muninsSocket;
    }

    @Override
    public long setUptime() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean startConnection() {
        SocketFactory ss = getLevel().find(SocketFactory.class); 
        try {
            muninsSocket = ss.createSocket(getHostName(), port);
        } catch (IOException e) {
            log(Level.ERROR, e, "Connection error", e);
            return false;
        }
        return true;
    }

    @Override
    public void stopConnection() {
        try {
            muninsSocket.close();
        } catch (IOException e) {
        }		
    }

}
