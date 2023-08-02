package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.FEATURES;
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
            byte version = buffer.getByte();
            byte licensed = buffer.getByte();
            short featuresMask = buffer.getShort();
            Set<FEATURES> features = FEATURES.resolveMask(featuresMask);
            serverInfo = ServerInfo.builder().features(features).licensed(licensed).version(version).build();
            logger.debug("Starting with status={} version={} licensed={} features={}", 
                         status, serverInfo.getVersion(), serverInfo.getLicensed(), serverInfo.getFeatures());
        }
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(0);
        buffer.putByte(serverInfo.getVersion());
        buffer.putByte(serverInfo.getLicensed());
        buffer.putShort(FEATURES.buildMask(serverInfo.getFeatures()));
    }

    @Override
    public int bufferSize() {
        return 20;
    }

}
