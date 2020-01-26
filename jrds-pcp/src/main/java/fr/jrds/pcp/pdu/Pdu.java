package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PCPException;
import lombok.Getter;
import lombok.Setter;

public abstract class Pdu {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    @Getter @Setter
    int from = 0;

    public final void write(MessageBuffer buffer) {
        buffer.putInt(0);
        buffer.putInt(getType().code);
        buffer.putInt(from);
        fill(buffer);
        buffer.putInt(0, buffer.position());
    }

    public final void read(MessageBuffer buffer) throws PCPException {
        int size = buffer.getInt();
        int type = buffer.getInt();
        from = buffer.getInt();
        logger.trace("PDU header: size={} type={} from={}", size, type, from);
        parse(buffer);
    }

    public abstract PDU_TYPE getType();

    public boolean isErrorPdu() {
        return false;
    }

    protected void fill(MessageBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    protected void parse(MessageBuffer buffer) throws PCPException {
        throw new UnsupportedOperationException();
    }

    public int bufferSize() {
        throw new UnsupportedOperationException("Parsed only PDU");
    }

}
