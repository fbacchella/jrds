package jrds.probe.munin;

import java.io.IOException;
import java.net.Socket;

import org.apache.log4j.Level;

import jrds.factories.ProbeBean;
import jrds.starter.Connection;
import jrds.starter.SocketFactory;

@ProbeBean({"port"})
public class MuninConnection extends Connection<Socket> {
    static final int DEFAULTMUNINPORT = 4949;
    private Socket muninsSocket = null;
    private int port = DEFAULTMUNINPORT;

    public MuninConnection() {
        super();
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
