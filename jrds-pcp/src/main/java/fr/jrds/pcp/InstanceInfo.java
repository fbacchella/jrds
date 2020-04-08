package fr.jrds.pcp;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data @Builder
public class InstanceInfo {

    @Getter
    private final int instance;

    @Getter
    private final String name;

}
