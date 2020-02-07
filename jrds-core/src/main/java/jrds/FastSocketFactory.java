package jrds;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;

import javax.net.SocketFactory;

public class FastSocketFactory extends SocketFactory {

    private static class FastSocket extends Socket {
        private final int timeout;
        public FastSocket(int timeout, InetAddress address, int port, InetAddress localAddr,
                          int localPort)
                        throws IOException {
            super(address, port, localAddr, localPort);
            this.timeout = timeout;
            setSoTimeout(timeout);
            setTcpNoDelay(true);
        }
        public FastSocket(int timeout, InetAddress address, int port) throws IOException {
            super(address, port);
            this.timeout = timeout;
            setSoTimeout(timeout);
            setTcpNoDelay(true);
        }
        public void connect(SocketAddress endpoint) throws IOException {
            super.connect(endpoint, timeout);
        }
    }
    
    private final int timeout;
    
    public FastSocketFactory(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public Socket createSocket(String host, int port)
                    throws IOException, UnknownHostException {
        Socket s = new FastSocket(timeout, InetAddress.getByName(host), port);
        return s;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new FastSocket(timeout, host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
                               int localPort)
                    throws IOException, UnknownHostException {
        return new FastSocket(timeout, InetAddress.getByName(host), port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort)
                    throws IOException {
        return new FastSocket(timeout, address, port, localAddress, localPort);
    }
    
    RMIClientSocketFactory getRMIClientSocketFactory() {
        return new RMIClientSocketFactory() {

            @Override
            public Socket createSocket(String host, int port)
                            throws IOException {
                return new FastSocket(timeout, InetAddress.getByName(host), port);
            }
        };
    }

}
