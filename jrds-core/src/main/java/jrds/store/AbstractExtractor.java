package jrds.store;

import java.util.HashMap;
import java.util.Map;

import org.rrd4j.core.DataHolder;

public abstract class AbstractExtractor<Source> implements Extractor {

    protected Map<String, String> sources = new HashMap<String, String>();

    public AbstractExtractor() {
        super();
    }

    public abstract void release();

    /*
     * (non-Javadoc)
     * 
     * @see jrds.store.Extractor#addSource(java.lang.String, java.lang.String)
     */
    @Override
    public void addSource(String name, String dsName) {
        sources.put(name, dsName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.store.ExtractorInterface#getNames()
     */
    @Override
    public final String[] getNames() {
        return sources.keySet().toArray(new String[] {});
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.store.ExtractorInterface#getDsNames()
     */
    @Override
    public final String[] getDsNames() {
        return sources.values().toArray(new String[] {});
    }

    @Override
    public abstract String getPath();

    @Override
    public abstract void fill(DataHolder dp, ExtractInfo ei);

    /*
     * (non-Javadoc)
     * 
     * @see jrds.store.ExtractorInterface#getColumnCount()
     */
    @Override
    public abstract int getColumnCount();

}
