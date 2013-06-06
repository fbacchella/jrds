package jrds.store;

import java.util.HashMap;
import java.util.Map;

import org.rrd4j.data.Plottable;

public abstract class AbstractExtractor<Source> implements Extractor {
    private final Map<Integer, Source> plotted = new HashMap<Integer, Source>();

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

    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getValues(jrds.store.ExtractInfo)
     */
    @Override
    public abstract double[][] getValues(ExtractInfo ei);
    
    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getSourceValues(jrds.store.ExtractInfo)
     */
    @Override
    public abstract double[] getSourceValues(ExtractInfo ei);

    protected Source getSource(ExtractInfo ei) {
        int signature = getSignature(ei);
        if(! plotted.containsKey(signature) ) {
            plotted.put(signature, newPlottableSource(ei));
        }
        return plotted.get(signature);
    }
    
    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getPlottable(jrds.store.ExtractInfo)
     */
    @Override
    public final Plottable getPlottable(ExtractInfo ei) {
        Source s = getSource(ei);
        return newPlottable(s, ei);
    }
    
    protected abstract Source newPlottableSource(ExtractInfo ei);

    protected abstract Plottable newPlottable(Source s, ExtractInfo ei);

    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getTimestamps(jrds.store.ExtractInfo)
     */
    @Override
    public abstract long[] getTimestamps(ExtractInfo ei);

    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getColumnCount()
     */
    @Override
    public abstract int getColumnCount();
    
    protected abstract int getSignature(ExtractInfo ei);
    
    /* (non-Javadoc)
     * @see jrds.store.ExtractorInterface#getValue(jrds.store.ExtractInfo)
     */
    @Override
    public abstract double getValue(ExtractInfo ei);
    
}
