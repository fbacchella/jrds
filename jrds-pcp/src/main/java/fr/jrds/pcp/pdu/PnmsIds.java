package fr.jrds.pcp.pdu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PmId;
import lombok.Getter;

public class PnmsIds extends Pdu {

    private List<PmId> ids;

    @Getter
    private int status;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.PNMS_IDS;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        status = buffer.getInt();
        int count = buffer.getInt();
        ids =  new ArrayList<>(count);
        for (int i = 0; i < count ; i++) {
            ids.add(i, new PmId(buffer.getInt()));
        }
        ids = Collections.unmodifiableList(ids);
    }

    public List<PmId> getIds() {
        return ids;
    }

}
