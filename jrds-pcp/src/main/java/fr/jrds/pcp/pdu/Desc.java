package fr.jrds.pcp.pdu;

import fr.jrds.pcp.MessageBuffer;
import fr.jrds.pcp.PmId;
import fr.jrds.pcp.VALUE_TYPE;
import lombok.Getter;

public class Desc extends Pdu {

    @Getter
    PmId pmid;
    
    @Getter
    VALUE_TYPE valueType;
    
    @Getter
    int indom;
    
    @Getter
    int sem;
    
    @Getter
    int units;

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.DESC;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        pmid = new PmId(buffer.getInt());
        valueType = VALUE_TYPE.values()[buffer.getInt()];
        indom = buffer.getInt();
        sem = buffer.getInt();
        units = buffer.getInt();
    }

}
