package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.FEATURES;
import fr.jrds.pcp.MessageBuffer;
import lombok.Getter;

public class Start extends Pdu {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    private byte version;

    @Getter
    private byte licensed;

    @Getter
    private int status;

    @Getter
    Set<FEATURES> features;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.START;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        status = buffer.getInt();
        if (status == 0) {
            version = buffer.getByte();
            licensed = buffer.getByte();
            short featuresMask = buffer.getShort();
            features = Collections.unmodifiableSet(FEATURES.resolveMask(featuresMask));
            logger.debug("Starting with status={} version={} licensed={} features={}", status, version, licensed, features);
        };
    }

}
