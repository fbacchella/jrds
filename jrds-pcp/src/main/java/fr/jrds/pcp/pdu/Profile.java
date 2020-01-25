package fr.jrds.pcp.pdu;

import fr.jrds.pcp.MessageBuffer;

public class Profile extends Pdu {

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.PROFILE;
    }

    @Override
    public int bufferSize() {
        return 28;
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
    }

}
