package jrds.starter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

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

    private static class FastSocket extends Socket {
        private final int timeout;
        public FastSocket(int timeout) throws SocketException {
            super();
            this.timeout = timeout;
            setSoTimeout(timeout);
            setTcpNoDelay(true);
        }
        public void connect(SocketAddress endpoint) throws IOException {
            super.connect(endpoint, timeout);
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        if(!isStarted()) {
            return null;
        } else {
            return new FastServerSocket(port, getTimeout() * 1000);
        }
    }

    public Socket createSocket(String host, int port) throws IOException {
        if(!isStarted())
            return null;

        Socket s = getSocket();
        s.connect(new InetSocketAddress(host, port));

        return s;
    }

    public Socket createSocket(StarterNode host, int port) throws IOException {
        if(!isStarted())
            return null;

        Resolver r = host.find(Resolver.class);
        if(r == null || !r.isStarted())
            return null;

        Socket s = getSocket();
        s.connect(new InetSocketAddress(r.getInetAddress(), port));
        return s;
    }

    public Socket createSocket() throws IOException {
        if(!isStarted()) {
            return null;
        } else {
            return getSocket();
        }
    }

    private Socket getSocket() throws SocketException {
        return new FastSocket(getTimeout() * 1000);
    }

    /**
     * @return the timeout
     */
    public int getTimeout() {
        return getLevel().getTimeout();
    }

}
