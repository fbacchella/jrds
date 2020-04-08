package fr.jrds.pcp;

import lombok.Data;
import lombok.Getter;

@Data
public class PmId {

    @Getter
    private final int id;

    public PmId(int pmid) {
        this.id = pmid;
    }

    @Override
    public String toString() {
        short domain = (short) ((id >> 22) & ((2<<9) -1));
        short cluster = (short) ((id >> 10) & ((2<<12) -1));
        short item = (short) (id & ((2<<10) -1));
        return "" + Domains.instance.getDomain(domain) + "." + cluster + "." + item;
    }

    
}
