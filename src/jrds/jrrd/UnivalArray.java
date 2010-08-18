package jrds.jrrd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class UnivalArray {
    RRDFile file;
    ByteBuffer buffer;

    public UnivalArray(RRDFile file, int size) throws IOException {
        super();
        this.file = file;
        buffer = ByteBuffer.allocate(size * 8);
        if(file.isBigEndian())
            buffer.order(ByteOrder.BIG_ENDIAN);
        else
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        file.read(buffer);
    }
    
    public long getLong(Enum<?> e) {
        buffer.position(8 * e.ordinal());
        return buffer.getLong();
    }

    public double getDouble(Enum<?> e) {
        buffer.position(8 * e.ordinal());
        return buffer.getDouble();
    }

}
