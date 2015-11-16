package jrds.starter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Date;
import java.util.Set;

import org.apache.log4j.Level;

public class SocketFactory extends Starter implements RMIClientSocketFactory {

    private final Selector selector;

    public SocketFactory() throws IOException{
        super();
        selector = Selector.open();
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        if(! isStarted())
            return null;

        ServerSocket s = new ServerSocket(port) {

            /* (non-Javadoc)
             * @see java.net.ServerSocket#accept()
             */
            @Override
            public Socket accept() throws IOException {
                Socket accepted = super.accept();
                accepted.setTcpNoDelay(true);
                return accepted;
            }

        };
        s.setSoTimeout(getTimeout() * 1000);
        return s;
    }

    public Socket createSocket(String host, int port) throws IOException {
        if(! isStarted())
            return null;

        SocketChannel s = getSocket();
        return tryConnect(s, new InetSocketAddress(host, port));
    }

    public Socket createSocket(StarterNode host, int port) throws IOException {
        if(! isStarted())
            return null;

        Resolver r = host.find(Resolver.class);
        if(r == null || ! r.isStarted())
            return null;

        SocketChannel s = getSocket();
        return tryConnect(s, new InetSocketAddress(r.getInetAddress(), port));
    }

    public Socket createSocket() throws IOException {
        if(! isStarted())
            return null;
        SocketChannel s = getSocket();
        s.configureBlocking(true);
        return s.socket();
    }

    private SocketChannel getSocket() throws IOException {
        SocketChannel s = SocketChannel.open();
        s.configureBlocking(false);
        s.setOption(StandardSocketOptions.TCP_NODELAY, true);
        s.socket().setSoTimeout(getTimeout());
        return s;
    }

    private Socket tryConnect(SocketChannel s, InetSocketAddress addr) throws IOException {
        log(Level.DEBUG,"connecting a socket channel to %s", addr);
        SelectionKey key = s.register(selector, SelectionKey.OP_CONNECT);  
        long timeout = getTimeout() * 1000;
        try {
            s.connect(addr);
            while(isStarted() && s.isRegistered() && ! s.finishConnect() && timeout > 0) {
                long start = new Date().getTime();
                selector.select(timeout);
                long end = new Date().getTime();
                timeout -= (end - start);
                log(Level.WARN, "connect selected ?");        
            }
        } finally {
            key.cancel();
        }
        if( ! s.isConnected()) {
            throw new InterruptedByTimeoutException();
        }
        // Others might expect a blocking socket (like RMI)
        s.configureBlocking(true);
        return s.socket();

    }
    /**
     * @return the timeout
     */
    public int getTimeout() {
        return getLevel().getTimeout();
    }

    @Override
    public void stop() {
        super.stop();
        Set<SelectionKey> keyset = selector.keys();
        selector.wakeup();
        synchronized(keyset) {
            for(SelectionKey k: keyset) {
                k.cancel();
            }            
        }
    }

}
