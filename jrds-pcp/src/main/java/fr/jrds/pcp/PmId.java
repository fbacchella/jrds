package fr.jrds.pcp;

import lombok.Data;
import lombok.Getter;

@Data
public class PmId {

    /*
     * From src/include/pcp/libpcp.h:
     * 
     * Internally, this is how to decode a PMID!
     * - flag is to denote state internally in some operations
     * - domain is usually the unique domain number of a PMDA, but see
     *   below for some special cases
     * - cluster and item together uniquely identify a metric within a domain
     */

    @Getter
    private final int id;

    public PmId(int pmid) {
        this.id = pmid;
    }

}
