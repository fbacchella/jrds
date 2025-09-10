package jrds;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.rmi.server.RMIClientSocketFactory;
import java.time.Duration;

import javax.net.SocketFactory;

public class FastSocketFactory extends SocketFactory {

    private static class FastSocket extends Socket {
        private final Duration timeout;
        /**
         * Only this creator is allowed, otherwise connect will be called with a 0 (uninitialized) value
         * @param timeout
         * @throws SocketException
         */
        public FastSocket(Duration timeout) throws SocketException {
            this.timeout = timeout;
            setSoTimeout((int) timeout.toMillis());
            setTcpNoDelay(true);
        }
        public FastSocket(int timeout) throws SocketException {
            this(Duration.ofSeconds(timeout));
        }
        public void connect(SocketAddress endpoint) throws IOException {
            super.connect(endpoint, (int) timeout.toMillis());
        }
    }

    private final Duration timeout;

    public FastSocketFactory(Duration timeout) {
        this.timeout = timeout;
    }

    public FastSocketFactory(int timeout) {
        this.timeout = Duration.ofSeconds(timeout);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
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
                               int localPort) throws IOException {
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
        return (host, port) -> {
            Socket s = new FastSocket(timeout);
            s.connect(new InetSocketAddress(InetAddress.getByName(host), port));
            return s;
        };
    }

}
