package fr.jrds.pcp;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.pdu.PDU_TYPE;
import fr.jrds.pcp.pdu.PdpError;
import fr.jrds.pcp.pdu.Pdu;
import lombok.Getter;
import lombok.Setter;

public class ProtocolHandler implements Closeable {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter @Setter
    int from = 0;

    private final Transport transport;
    private final ByteBuffer sizeBuffer;

    public ProtocolHandler(Transport transport) {
        this.transport = transport;
        sizeBuffer = ByteBuffer.allocate(4);
        sizeBuffer.order(transport.getByteOrder());
    }

    protected void send(Pdu toSend) throws IOException, InterruptedException {
        logger.debug("Sending pdu type: {}", toSend.getType());
        MessageBuffer buffer = new MessageBuffer(toSend.bufferSize(), transport.getByteOrder());
        toSend.setFrom(from);
        toSend.write(buffer);
        buffer.flip();
        transport.write(buffer.getView());
        buffer = null;
    }

    @SuppressWarnings("unchecked")
    protected <P extends Pdu> P receive() throws IOException, PCPException, InterruptedException {
        sizeBuffer.clear();
        transport.read(sizeBuffer);
        sizeBuffer.flip();
        int pduSizeToRead = sizeBuffer.getInt();
        if (pduSizeToRead < 0) {
            throw new IOException("Inconcistent PDU: too big");
        }
        MessageBuffer buffer = new MessageBuffer(pduSizeToRead, transport.getByteOrder());
        buffer.putInt(pduSizeToRead);
        buffer.read(transport);
        buffer.flip();
        Pdu receivedPdu = PDU_TYPE.Resolve(buffer.getView());
        logger.debug("Received pdu type: {}", receivedPdu.getType());
        receivedPdu.read(buffer);
        if (receivedPdu.isErrorPdu()) {
            PdpError error = ((PdpError) receivedPdu);
            throw new PCPException(error.getError());
        } else {
            return (P) receivedPdu;
        } 
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}
