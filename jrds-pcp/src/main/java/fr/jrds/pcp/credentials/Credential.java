package fr.jrds.pcp.credentials;

import fr.jrds.pcp.MessageBuffer;

public abstract class Credential {

    public final void write(MessageBuffer buffer) {
        buffer.putByte((byte) getType());
        fill(buffer);
    }

    public abstract int getType();
    protected abstract void fill(MessageBuffer buffer);

}
