package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.ServerInfo;
import lombok.Getter;
import lombok.Setter;

public class Start extends Pdu {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter @Setter
    ServerInfo serverInfo;
    
    @Getter
    private int status;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.START;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        status = buffer.getInt();
        if (status == 0) {
            serverInfo = new ServerInfo();
            serverInfo.parse(buffer);
            logger.debug("Starting with status={} version={} licensed={} features={}", 
                         status, serverInfo.getVersion(), serverInfo.getLicensed(), serverInfo.getFeatures());
        };
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(0);
        serverInfo.fill(buffer);
    }

    @Override
    public int bufferSize() {
        return 20;
    }

}
