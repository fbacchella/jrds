package jrds.store;

import java.util.Date;
import java.util.Map;

import org.slf4j.event.Level;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;

public abstract class AbstractStore<SO> implements Store<SO> {
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
    public abstract SO getStoreObject();

    @Override
    public abstract void closeStoreObject(SO object);

    public void log(Level l, Throwable e, String format, Object... elements) {
        p.log(l, e, format, elements);
    }

    public void log(Level l, String format, Object... elements) {
        p.log(l, format, elements);
    }

}
