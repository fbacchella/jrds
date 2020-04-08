package fr.jrds.pcp.pdu;

import fr.jrds.pcp.MessageBuffer;
import lombok.Setter;

public class PnmsTraverse extends Pdu {

    @Setter
    private String name = null;

    @Setter
    private int subtype;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.PMNS_TRAVERSE;
    }

    @Override
    public int bufferSize() {
        return 20 + (name != null ? name.length() + 3: 0);
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(subtype);
        buffer.putString(name);
    }

}
