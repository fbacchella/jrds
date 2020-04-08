package fr.jrds.pcp;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * @author Fabrice Bacchella
 * Described in src/include/pcp/pmapi.h, called pmDesc
 */
@Builder @Data
public class PmDesc {
    
    @Getter
    private final PmId pmid;
    @Getter
    private final VALUE_TYPE type;
    @Getter
    private final int indom;
    @Getter
    private final int sem;
    @Getter
    private final int units;

}
