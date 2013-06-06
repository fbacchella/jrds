package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.Probe;
import jrds.Probe.JrdsSample;

public class SampleCache extends AbstractStore<Map<String, Number>, Object> {
    private final Map<String, Number> cache;

    public SampleCache(Probe<?,?> p, Map<String, Number> cache) {
        super(p);
        this.cache = cache;
    }

    @Override
    public void commit(JrdsSample sample) {
        cache.putAll(sample);
    }

    @Override
    public Map<String, Number> getLastValues() {
        return cache;
    }

    @Override
    public boolean checkStoreFile() {
        return true;
    }

    @Override
    public Date getLastUpdate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EmptyExtractor fetchData() {
        return new EmptyExtractor();
    }

    @Override
    public Map<String, Number> getStoreObject() {
        return cache;
    }

    @Override
    public void closeStoreObject(Object object) {

    }

}
