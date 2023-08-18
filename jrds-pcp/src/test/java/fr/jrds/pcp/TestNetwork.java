package fr.jrds.pcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestNetwork extends Tester {

    @Test(expected = IOException.class, timeout=3000)
    public void timeout() throws IOException, InterruptedException {
        try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getByName("169.254.1.1"), 44321), 2000)) {
        }
    }

    @Test(expected = InterruptedException.class, timeout=3000)
    public void interrupted() throws IOException, InterruptedException {
        Thread current = Thread.currentThread();
        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(1000);
                current.interrupt();
            } catch (InterruptedException e) {
            }
        });
        stopper.start();
        try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getByName("169.254.1.1"), 44321), 2000)) {
        }
    }

    @Test(expected = ClosedChannelException.class, timeout=500)
    public void connectRefused() throws IOException, PCPException, InterruptedException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Runnable r2 = () -> {
                try {
                    Socket socket = serverSocket.accept();
                    socket.close();
                } catch (IOException e) {
                }
            };
            Thread t = new Thread(r2);
            t.setDaemon(true);
            t.start();
            try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getLocalHost(), serverSocket.getLocalPort()), 2000)) {
                cnx.startClient();
            } finally {
                t.interrupt();
            }
        }
    }

    @Test(timeout=500)
    public void connectStart() throws IOException, PCPException, InterruptedException {
        try (ServerSocketChannel listenSocket = ServerSocketChannel.open()) {
            InetSocketAddress listenAddr = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            listenSocket.bind(listenAddr);

            Runnable r2 = () -> {
                try {
                    SocketChannel client = listenSocket.accept();
                    try (Transport clientTransport = new PlainTcpTransport(client, 500);
                                    Connection cnx = new Connection(clientTransport)) {
                        ServerInfo si = ServerInfo.builder().features(Collections.singleton(FEATURES.CREDS_REQD)).licensed((byte)0).version((byte)2).build();
                        cnx.startServer(si);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            };
            Thread t = new Thread(r2);
            t.setDaemon(true);
            t.start();
            try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getLocalHost(), listenSocket.socket().getLocalPort()), 2000)) {
                ServerInfo si = cnx.startClient();
                Set<FEATURES> features = si.getFeatures();
                Assert.assertEquals(Collections.singleton(FEATURES.CREDS_REQD), features);
                Assert.assertEquals(2, si.getVersion());
            } finally {
                t.interrupt();
            }
        }
    }

}
