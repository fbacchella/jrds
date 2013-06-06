package jrds.store;

import jrds.Probe;
import jrds.factories.ProbeBean;

public class RrdDbStoreFactory extends AbstractStoreFactory<RrdDbStore> {

    @Override
    public RrdDbStore create(Probe<?, ?> p) {
        return new RrdDbStore(p);
    }

}
