package jrds.probe.munin;

import jrds.factories.ProbeBean;
import jrds.probe.IndexedProbe;

@ProbeBean({"index"})
public class MuninIndexed extends Munin implements IndexedProbe {
    String indexKey;

    public boolean configure(String indexKey) {
        this.indexKey = indexKey;
        return super.configure();
    }

    public String getIndexName() {
        return indexKey;
    }

    /**
     * @return the indexKey
     */
    public String getIndex() {
        return indexKey;
    }

    /**
     * @param indexKey the indexKey to set
     */
    public void setIndex(String indexKey) {
        this.indexKey = indexKey;
    }

}
