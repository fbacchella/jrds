package jrds.store;

import org.rrd4j.core.jrrd.RRDatabase;

import jrds.Probe;

public class RRDToolStoreFactory extends AbstractStoreFactory<RRDatabase, RRDToolStore> {

    @Override
    public RRDToolStore create(Probe<?, ?> p) {
        return new RRDToolStore(p);
    }

}
