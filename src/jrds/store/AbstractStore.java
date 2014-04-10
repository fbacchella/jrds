package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;

import org.apache.log4j.Level;

public abstract class AbstractStore<StoreObject> implements Store {
    protected final Probe<?, ?> p;

    public AbstractStore(Probe<?, ?> p) {
        super();
        this.p = p;
    }

    @Override
    public abstract void commit(JrdsSample sample);
    
    @Override
    public abstract Map<String, Number> getLastValues();
    
    @Override
    public abstract boolean checkStoreFile(ArchivesSet archives);
    
    @Override
    public abstract Date getLastUpdate();
    
    @Override
    public abstract StoreObject getStoreObject();
    
    @Override
    public abstract void closeStoreObject(Object object);

    public void log(Level l, Throwable e, String format, Object... elements) {
        p.log(l, e, format, elements);
    }

    public void log(Level l, String format, Object... elements) {
        p.log(l, format, elements);
    }

}
