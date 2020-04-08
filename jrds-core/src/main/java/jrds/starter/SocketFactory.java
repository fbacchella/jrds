package jrds.starter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import jrds.FastSocketFactory;

public class SocketFactory extends Starter {

    private static class FastServerSocket extends ServerSocket {
        private final int timeout;
        public FastServerSocket(int port, int timeout) throws IOException {
            super(port);
            this.timeout = timeout;
        }

        @Override
        public Socket accept() throws IOException {
            Socket accepted = super.accept();
            accepted.setTcpNoDelay(true);
            accepted.setSoTimeout(timeout);

            return accepted;
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        if(!isStarted()) {
            return null;
        } else {
            return new FastServerSocket(port, getTimeout() * 1000);
        }
    }

    private final FastSocketFactory socketFactory;

    public SocketFactory(int timeout) {
        this.socketFactory = new FastSocketFactory(timeout);
    }

    public javax.net.SocketFactory getFactory() {
        return socketFactory;
    }
    
    @Deprecated
    public Socket createSocket(String host, int port) throws IOException {
        if(!isStarted())
            return null;

        return socketFactory.createSocket(host, port);
    }

    @Deprecated
    public Socket createSocket(StarterNode host, int port) throws IOException {
        if(!isStarted())
            return null;

        Resolver r = host.find(Resolver.class);
        if(r == null || !r.isStarted())
            return null;

        return socketFactory.createSocket(r.getInetAddress(), port);
    }

    @Deprecated
    public Socket createSocket() throws IOException {
        if(!isStarted()) {
            return null;
        } else {
            return socketFactory.createSocket();
        }
    }

    /**
     * @return the timeout
     */
    @Deprecated
    public int getTimeout() {
        return getLevel().getTimeout();
    }

}
