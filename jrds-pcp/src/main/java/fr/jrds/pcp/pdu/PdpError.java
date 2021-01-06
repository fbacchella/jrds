package fr.jrds.pcp.pdu;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jrds.pcp.ERROR;
import fr.jrds.pcp.MessageBuffer;
import lombok.Getter;

public class PdpError extends Pdu {

    static private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    private ERROR error;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.ERROR;
    }

    public boolean isErrorPdu() {
        return true;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        int errnum = buffer.getInt();
        logger.debug("errnum={}", errnum);
        error = ERROR.errors.get(errnum);
    }

}
