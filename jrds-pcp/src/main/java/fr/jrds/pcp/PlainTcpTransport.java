package fr.jrds.pcp;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlainTcpTransport implements Transport {
    
    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Selector selector = Selector.open();
    private final SocketChannel soc;
    private final Transport.Waiter waiter;
    
    public PlainTcpTransport(InetSocketAddress isa, long timeout) throws IOException, InterruptedException {
        soc = SocketChannel.open();
        waiter = (op) -> {
            try {
                SelectionKey key = soc.register(selector, op);
                selector.select(timeout);
                if ( (key.readyOps() & op) == 0) {
                    throw new InterruptedException("Timeout waiting IO");
                } else if (op == SelectionKey.OP_CONNECT) {
                    soc.finishConnect();
                }
            } catch (ClosedChannelException e) {
                throw new IllegalStateException("Closed channel");
            }
        };
        soc.setOption(StandardSocketOptions.TCP_NODELAY, true);
        soc.configureBlocking(false);
        soc.connect(isa);
        waiter.waitFor(SelectionKey.OP_CONNECT);
    }

    @Override
    public void read(ByteBuffer buffer) throws InterruptedException, IOException {
        int read = 0;
        while (buffer.remaining() > 0) {
            waiter.waitFor(SelectionKey.OP_READ);
            int bytesReads = soc.read(buffer);
            if (bytesReads <= 0) {
                break;
            }
            read += bytesReads;
        }
        logger.trace("Read {} bytes", read);
    }

    @Override
    public void write(ByteBuffer buffer) throws InterruptedException, IOException {
        waiter.waitFor(SelectionKey.OP_WRITE);
        int wrote = soc.write(buffer);
        logger.trace("Wrote {} bytes", wrote);
    }

    @Override
    public Waiter getWaiter() {
        return waiter;
    }

    @Override
    public ByteOrder getByteOrder() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public void close() throws IOException {
        soc.close();
    }

}
