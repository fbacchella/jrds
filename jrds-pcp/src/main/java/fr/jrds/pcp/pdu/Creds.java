package fr.jrds.pcp.pdu;

import java.util.ArrayList;
import java.util.List;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.credentials.Credential;

public class Creds extends Pdu {

    private final List<Credential> creds = new ArrayList<>(1);

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.CREDS;
    }

    public void addCred(Credential nc) {
        creds.add(nc);
    }

    @Override
    public int bufferSize() {
        return 20;
    }

    @Override
    protected void fill(MessageBuffer buffer) {
        buffer.putInt(creds.size());
        for (Credential i: creds) {
            i.write(buffer);
        }
    }

}
