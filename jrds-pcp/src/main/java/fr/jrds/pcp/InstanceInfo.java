package fr.jrds.pcp;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class InstanceInfo {

    @Getter @Setter(AccessLevel.NONE)
    int instance;

    @Getter@Setter(AccessLevel.NONE)
    String name;

    public void parse(MessageBuffer buffer) {
        instance = buffer.getInt();
        name = buffer.getString();
    }

}
