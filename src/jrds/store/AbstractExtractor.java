package jrds.store;

import java.util.Collection;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public abstract class AbstractExtractor<Source> implements Extractor {

    public AbstractExtractor() {
        super();
    }

    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getNames()
     */
    @Override
    public abstract String[] getNames();

    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getDsNames()
     */
    @Override
    public abstract String[] getDsNames();

    @Override
    public abstract void fill(DataProcessor dp, ExtractInfo ei, Collection<String> sources);

    @Override
    public abstract void fill(RrdGraphDef gd, ExtractInfo ei, Collection<String> sources);

    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getColumnCount()
     */
    @Override
    public abstract int getColumnCount();

    protected abstract int getSignature(ExtractInfo ei);

}
