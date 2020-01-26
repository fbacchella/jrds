package fr.jrds.pcp.pdu;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PmDesc;
import fr.jrds.pcp.PmId;
import fr.jrds.pcp.VALUE_TYPE;
import lombok.Getter;

public class Desc extends Pdu {

    @Getter
    private PmDesc desc;
    
    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.DESC;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        PmId pmid = new PmId(buffer.getInt());
        VALUE_TYPE valueType = VALUE_TYPE.values()[buffer.getInt()];
        int indom = buffer.getInt();
        int sem = buffer.getInt();
        int units = buffer.getInt();
        desc = PmDesc.builder().pmid(pmid).type(valueType).indom(indom).sem(sem).units(units).build();
    }

}
