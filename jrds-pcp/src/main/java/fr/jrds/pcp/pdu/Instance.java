package fr.jrds.pcp.pdu;

import java.util.ArrayList;
import java.util.List;

import fr.jrds.pcp.InstanceInstance;
import fr.jrds.pcp.MessageBuffer;
import lombok.Getter;

public class Instance extends Pdu {

    @Getter
    private int instanceDomain;
    
    @Getter
    private List<InstanceInstance> instances = new ArrayList<>();

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.INSTANCE;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        instanceDomain = buffer.getInt();
        int count = buffer.getInt();
        for (int i = 0; i < count ; i++) {
            InstanceInstance ii = new InstanceInstance();
            ii.parse(buffer);
            instances.add(ii);
        }
    }

}
