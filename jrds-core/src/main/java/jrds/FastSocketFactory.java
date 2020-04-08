package jrds;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;

import javax.net.SocketFactory;

public class FastSocketFactory extends SocketFactory {

    private static class FastSocket extends Socket {
        private final int timeout;
        /**
         * Only this creator is allowed, otherwise connect will be called with a 0 (uninitialized) value
         * @param timeout
         * @throws SocketException
         */
        public FastSocket(int timeout) throws SocketException {
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
        this.timeout = timeout * 1000;
    }

    @Override
    public Socket createSocket(String host, int port)
                    throws IOException, UnknownHostException {
        Socket s = new FastSocket(timeout);
        s.connect(new InetSocketAddress(InetAddress.getByName(host), port));
        return s;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket s = new FastSocket(timeout);
        s.connect(new InetSocketAddress(host, port));
        return s;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
                               int localPort)
                                               throws IOException, UnknownHostException {
        Socket s = new FastSocket(timeout);
        s.connect(new InetSocketAddress(host, port));
        return s;
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort)
                                               throws IOException {
        Socket s = new FastSocket(timeout);
        s.bind(new InetSocketAddress(localAddress, localPort) );
        s.connect(new InetSocketAddress(localAddress, port));
        return s;
    }

    @Deprecated
    RMIClientSocketFactory getRMIClientSocketFactory() {
        return new RMIClientSocketFactory() {

            @Override
            public Socket createSocket(String host, int port)
                            throws IOException {
                Socket s = new FastSocket(timeout);
                s.connect(new InetSocketAddress(InetAddress.getByName(host), port));
                return s;
            }
        };
    }

}
