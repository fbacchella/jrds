package fr.jrds.pcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestNetwork extends Tester {

    @Test(timeout=3000)
    public void timeout() {
        IOException ex = Assert.assertThrows(IOException.class, () -> {
            try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getByName("169.254.1.1"), 44321), Duration.ofSeconds(2))) {
            }
        });
        Assert.assertEquals("Interrupted by timeout", ex.getMessage());
    }

    @Test(timeout=3000)
    public void interrupted(){
        Thread current = Thread.currentThread();
        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(1000);
                current.interrupt();
            } catch (InterruptedException e) {
            }
        });
        stopper.start();
        Assert.assertThrows(InterruptedException.class, () -> {
            try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getByName("169.254.1.1"), 44321), Duration.ofSeconds(2))) {
            }
        });
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
            try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getLocalHost(), serverSocket.getLocalPort()), Duration.ofSeconds(2000))) {
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
                    try (Transport clientTransport = new PlainTcpTransport(client, Duration.ofSeconds(500));
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
            try (Connection cnx = new Connection(new InetSocketAddress(InetAddress.getLocalHost(), listenSocket.socket().getLocalPort()), Duration.ofSeconds(2000))) {
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
