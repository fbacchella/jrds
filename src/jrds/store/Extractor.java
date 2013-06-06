package jrds.store;

import java.util.HashMap;
import java.util.Map;

import org.rrd4j.data.Plottable;

public abstract class Extractor<Source> {
    private final Map<Integer, Source> plotted = new HashMap<Integer, Source>();

    public Extractor() {
        super();
    }
    
    public abstract String[] getNames();

    public abstract String[] getDsNames();

    public abstract double[][] getValues(ExtractInfo ei);
    
    public abstract double[] getSourceValues(ExtractInfo ei);

    protected Source getSource(ExtractInfo ei) {
        int signature = getSignature(ei);
        if(! plotted.containsKey(signature) ) {
            plotted.put(signature, newPlottableSource(ei));
        }
        return plotted.get(signature);
    }
    
    public final Plottable getPlottable(ExtractInfo ei) {
        Source s = getSource(ei);
        return newPlottable(s, ei);
    }
    
    protected abstract Source newPlottableSource(ExtractInfo ei);

    protected abstract Plottable newPlottable(Source s, ExtractInfo ei);

    public abstract long[] getTimestamps(ExtractInfo ei);

    public abstract int getColumnCount();
    
    protected abstract int getSignature(ExtractInfo ei);
    
    public abstract double getValue(ExtractInfo ei);
    
}
