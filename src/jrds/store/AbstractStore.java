package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.Probe;

import org.apache.log4j.Level;

public abstract class AbstractStore<StoreObject, DataSource> {
    protected final Probe<?, ?> p;

    public AbstractStore(Probe<?, ?> p) {
        super();
        this.p = p;
    }

    public abstract void commit(Probe.JrdsSample sample);
    public abstract Map<String, Number> getLastValues();
    public abstract boolean checkStoreFile();
    public abstract Date getLastUpdate();
    public abstract StoreObject getStoreObject();
    public abstract void closeStoreObject(Object object);

    public void log(Level l, Throwable e, String format, Object... elements) {
        p.log(l, format, elements);
    }

    public void log(Level l, String format, Object... elements) {
        p.log(l, format, elements);
    }

    public abstract Extractor<DataSource> fetchData();
}
