package jrds.store;

import jrds.Probe;

public class RRDToolStoreFactory extends AbstractStoreFactory<RRDToolStore> {

    @Override
    public RRDToolStore create(Probe<?, ?> p) {
        return new RRDToolStore(p);
    }

}
