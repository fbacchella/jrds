package fr.jrds.pcp.pdu;

import java.time.Instant;

import fr.jrds.pcp.MessageBuffer;
import lombok.Setter;

public class InstanceReq extends Pdu {

    @Setter
    private int instanceDomain;

    @Setter
    private Instant timeValue = Instant.EPOCH;

    @Setter
    private int instance = 0xffffffff;

    @Setter
    private String instanceName = null;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.INSTANCE_REQ;
    }

    @Override
    public int bufferSize() {
        return 32 + instanceName.length() + 3;
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(instanceDomain);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(instance);
        buffer.putString(instanceName);
    }

}
