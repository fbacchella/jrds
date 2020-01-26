package fr.jrds.pcp;

import java.util.Collections;
import java.util.Set;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data @Builder
public class ServerInfo {


    @Getter @Builder.Default
    private byte version=2;

    @Getter @Builder.Default
    private byte licensed=0;

    @Getter
    Set<FEATURES> features;
    
    public ServerInfo() {
    }

    
    private ServerInfo(byte version, byte licensed, Set<FEATURES> features) {
        this.version = version;
        this.licensed = licensed;
        this.features = Collections.unmodifiableSet(features);
    }


    public void parse(MessageBuffer buffer) {
        version = buffer.getByte();
        licensed = buffer.getByte();
        short featuresMask = buffer.getShort();
        features = Collections.unmodifiableSet(FEATURES.resolveMask(featuresMask));
    }
    
    public void fill(MessageBuffer buffer) {
        buffer.putByte((byte)2);
        buffer.putByte((byte)1);
        buffer.putShort(FEATURES.buildMask(features));
    }

}
