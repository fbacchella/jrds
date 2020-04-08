package fr.jrds.pcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MessageBuffer {

    private final ByteBuffer buffer;

    public MessageBuffer(int size, ByteOrder bo) {
        buffer = ByteBuffer.allocate(size);
        buffer.order(bo);
    }

    public MessageBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    public void read(Transport t) throws InterruptedException, IOException {
        t.read(buffer);
    }

    int paddingDisplacement() {
        int position = buffer.position();
        if ((position % 4) != 0) {
            return 4 - (position % 4);
        } else {
            return 0;
        }
    }

    public String getString() {
        int length = buffer.getInt();
        if (length != 0) {
            byte[] strBuffer = new byte[length];
            buffer.get(strBuffer);
            buffer.position(buffer.position() + paddingDisplacement());
            return new String(strBuffer, StandardCharsets.US_ASCII);
        } else {
            return null;
        }
    }

    public void putString(String value) {
        if (value == null) {
            buffer.putInt(0);
        } else {
            buffer.putInt(value.length());
            buffer.put(value.getBytes(StandardCharsets.US_ASCII));
            for (int i= paddingDisplacement() ; i > 0 ; i--) {
                buffer.put((byte) 0x7e);
            }
        }
    }

    public int getInt() {
        return buffer.getInt();
    }

    public int position() {
        return buffer.position();
    }

    public void putInt(int value) {
        buffer.putInt(value);
    }

    public void putInt(int value, int position) {
        buffer.putInt(value, position);
    }

    public void putByte(byte value) {
        buffer.put(value);
    }

    public void putShort(short value) {
        buffer.putShort(value);
    }

    public void mark() {
        buffer.mark();
    }

    public void reset() {
        buffer.reset();
    }

    public byte getByte() {
        return buffer.get();
    }

    public void position(int newPosition) {
        buffer.position(newPosition);
    }

    public void getByte(byte[] dst, int offset, int length) {
        buffer.get(dst, offset, length);
    }

    public void getByte(byte[] dst) {
        buffer.get(dst);
    }

    public short getShort() {
        return buffer.getShort();
    }
    
    public ByteBuffer getView() {
        return buffer.asReadOnlyBuffer();
    }

    public void flip() {
        buffer.flip();
    }

}
