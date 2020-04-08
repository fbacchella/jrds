package fr.jrds.pcp.credentials;

import fr.jrds.pcp.MessageBuffer;

public class CVersion extends Credential {

    @Override
    public int getType() {
        return 1;
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putByte((byte) 2);
        buffer.putShort((short) 0);
    }

}
