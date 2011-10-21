package jrds.probe.munin;

import jrds.probe.IndexedProbe;

public class MuninIndexed extends Munin implements IndexedProbe {
    String indexKey;

    public boolean configure(String indexKey) {
        this.indexKey = indexKey;
        return super.configure();
    }

    public String getIndexName() {
        return indexKey;
    }
}
