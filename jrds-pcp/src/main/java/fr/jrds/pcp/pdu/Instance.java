package fr.jrds.pcp.pdu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.jrds.pcp.InstanceInfo;
import fr.jrds.pcp.MessageBuffer;
import lombok.Getter;

public class Instance extends Pdu {

    @Getter
    private int instanceDomain;
    
    @Getter
    private List<InstanceInfo> instances = new ArrayList<>();

    @Override
    public PDU_TYPE getType() {
        return PDU_TYPE.INSTANCE;
    }

    @Override
    protected void parse(MessageBuffer buffer) {
        instanceDomain = buffer.getInt();
        int count = buffer.getInt();
        for (int i = 0; i < count ; i++) {
            int instance = buffer.getInt();
            String name = buffer.getString();
            InstanceInfo ii = InstanceInfo.builder().instance(instance).name(name).build();
            instances.add(ii);
        }
        instances = Collections.unmodifiableList(instances);
    }

}
