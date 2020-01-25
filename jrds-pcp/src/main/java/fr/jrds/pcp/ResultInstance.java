package fr.jrds.pcp;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Explained in https://pcp.io/man/man5/LOGARCHIVE.5.html
 * @author Fabrice Bacchella
 *
 */
@Data
public class ResultInstance {

    @Getter @Setter(AccessLevel.NONE)
    int instance;

    @Setter(AccessLevel.NONE) @Getter(AccessLevel.NONE)
    Object value;
    
    public ResultInstance() {
    }

    public ResultInstance(ERROR error) {
        value = error;
    }

    public void parse(MessageBuffer buffer, VALFMT valfmt) {
        instance = buffer.getInt();
        switch(valfmt) {
        case INSITU:
            value = Integer.toUnsignedLong(buffer.getInt());
            break;
        case DPTR:
            value = dptr(buffer);
            break;
        case SPTR:
            throw new UnsupportedOperationException();
        }
    }

    private Object dptr(MessageBuffer buffer) {
        int instance_offset = buffer.getInt();
        buffer.mark();
        buffer.position(instance_offset * 4);
        byte type_value = buffer.getByte();
        VALUE_TYPE type = VALUE_TYPE.values()[type_value];
        byte[] valueLengthBytes = new byte[4];
        Arrays.fill(valueLengthBytes, (byte)0);
        buffer.getByte(valueLengthBytes, 1, 3);
        int valueLength = ByteBuffer.wrap(valueLengthBytes).getInt();
        byte[] valueBytes = new byte[valueLength - 4];
        buffer.getByte(valueBytes);
        Object value;
        switch (type) {
        case STRING:
            // A zero-terminated string
            value = new String(valueBytes, 0, valueBytes.length -1);
            break;
        case DOUBLE:
            value = ByteBuffer.wrap(valueBytes).getDouble();
            break;
        case FLOAT:
            value = ByteBuffer.wrap(valueBytes).getFloat();
            break;
        case U64:
        case I64:
            value = ByteBuffer.wrap(valueBytes).getLong();
            break;
        case I32:
        case U32:
            value = ByteBuffer.wrap(valueBytes).getInt();
        default:
            throw new UnsupportedOperationException(type.toString());
        }
        buffer.reset();
        return value;
    }

    @SuppressWarnings("unchecked")
    public <P> P getCheckedValue() throws PCPException {
        if (value instanceof ERROR) {
            throw new PCPException((ERROR)value);
        }
        return (P) value;
    }

}
