package fr.jrds.pcp;

import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;

@Data @Builder
public class ServerInfo {

    @Getter @Builder.Default
    private byte version=2;

    @Getter @Builder.Default
    private byte licensed=0;

    @Getter @Singular
    Set<FEATURES> features;

}
