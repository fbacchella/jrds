package fr.jrds.pcp;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Transport extends Closeable {
    
    @FunctionalInterface
    public static interface Waiter {
        public void waitFor(int op) throws InterruptedException, IOException;
    }

    public void read(ByteBuffer buffer) throws InterruptedException, IOException;
    public void write(ByteBuffer buffer) throws InterruptedException, IOException;
    public Waiter getWaiter();
    public ByteOrder getByteOrder();

}
