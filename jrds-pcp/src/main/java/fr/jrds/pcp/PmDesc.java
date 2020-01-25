package fr.jrds.pcp;

import fr.jrds.pcp.pdu.Desc;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Fabrice Bacchella
 * Described in src/include/pcp/pmapi.h, called pmDesc
 * <code>
 * typedef struct pmDesc {
 *   pmID        pmid;           * unique identifier 
 *   int         type;           * base data type (see below) 
 *   pmInDom     indom;          * instance domain 
 *   int         sem;            * semantics of value (see below) 
 *   pmUnits     units;          * dimension and units 
 * } pmDesc;
 * </code>
 */
@Builder
public class PmDesc {
    
    @Getter
    PmId pmid;
    @Getter
    VALUE_TYPE type;
    @Getter
    int indom;
    @Getter
    int sem;
    @Getter
    int units;
    
    static public PmDesc of(Desc d) {
        return PmDesc.builder().pmid(d.getPmid()).type(d.getValueType()).indom(d.getIndom()).sem(d.getSem()).units(d.getUnits()).build();
    }

}
