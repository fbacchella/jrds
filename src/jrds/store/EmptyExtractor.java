package jrds.store;

import org.rrd4j.data.Plottable;

public class EmptyExtractor extends Extractor<Object> {

    @Override
    public String[] getNames() {
        return new String[] {};
    }

    @Override
    public String[] getDsNames() {
        return new String[] {};
    }

    @Override
    public double[][] getValues(ExtractInfo ei) {
        return new double[][]{};
    }

    @Override
    protected Object newPlottableSource(ExtractInfo ei) {
        return null;
    }

    @Override
    protected Plottable newPlottable(Object s, ExtractInfo ei) {
        return new Plottable() {
            @Override
            public double getValue(long timestamp) {
                return Double.NaN;
            }
        };
    }

    @Override
    public long[] getTimestamps(ExtractInfo ei) {
        return new long[]{};
    }

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    protected int getSignature(ExtractInfo ei) {
        return ExtractInfo.get().hashCode();
    }

    @Override
    public double getValue(ExtractInfo ei) {
        return Double.NaN;
    }

    @Override
    public double[] getSourceValues(ExtractInfo ei) {
        return new double[]{};
    }

}
