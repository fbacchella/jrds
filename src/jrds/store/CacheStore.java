package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;

public class CacheStore extends AbstractStore<Map<String, Number>> {
    private final Map<String, Number> cache;
    private Date lastUpdate = new Date(0);

    public CacheStore(Probe<?,?> p, Map<String, Number> cache) {
        super(p);
        this.cache = cache;
    }

    @Override
    public void commit(JrdsSample sample) {
        cache.putAll(sample);
        lastUpdate = new Date();
    }

    @Override
    public Map<String, Number> getLastValues() {
        return cache;
    }

    @Override
    public boolean checkStoreFile(ArchivesSet archives) {
        return true;
    }

    @Override
    public Date getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public EmptyExtractor getExtractor() {
        return new EmptyExtractor();
    }

    @Override
    public Map<String, Number> getStoreObject() {
        return cache;
    }

    @Override
    public void closeStoreObject(Object object) {

    }

    @Override
    public String getPath() {
        return p.getQualifiedName();
    }

}
