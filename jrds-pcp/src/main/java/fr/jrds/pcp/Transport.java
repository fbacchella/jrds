package fr.jrds.pcp;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Transport extends Closeable {
    
    @FunctionalInterface
    interface Waiter {
        void waitFor(int op) throws InterruptedException, IOException;
    }

    void read(ByteBuffer buffer) throws InterruptedException, IOException;
    void write(ByteBuffer buffer) throws InterruptedException, IOException;
    Waiter getWaiter();
    ByteOrder getByteOrder();

}
