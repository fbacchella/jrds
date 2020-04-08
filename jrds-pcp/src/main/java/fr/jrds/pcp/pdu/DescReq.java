package fr.jrds.pcp.pdu;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PmId;
import lombok.Setter;

public class DescReq extends Pdu {

    @Setter
    private PmId id;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.DESC_REQ;
    }

    @Override
    public int bufferSize() {
        return 16;
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(id.getId());
    }

}
