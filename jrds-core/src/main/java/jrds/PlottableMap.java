package jrds;

import java.util.HashMap;

import org.rrd4j.data.IPlottable;

import jrds.store.ExtractInfo;

public abstract class PlottableMap extends HashMap<String, IPlottable> {
    public static class ProxyPlottable implements IPlottable {
        IPlottable real = new IPlottable() {
            @Override
            public double getValue(long timestamp) {
                return Double.NaN;
            }
        };

        @Override
        public double getValue(long timestamp) {
            return real.getValue(timestamp);
        }

        public void setReal(IPlottable real) {
            this.real = real;
        }
    };

    public static final PlottableMap Empty = new PlottableMap(0) {
        @Override
        public void configure(long start, long end, long step) {
        }

        @Override
        public IPlottable put(String key, IPlottable value) {
            throw new UnsupportedOperationException("read only empty map");
        }

        @Override
        public IPlottable get(Object key) {
            return null;
        }
    };

    public PlottableMap() {
        super();
    }

    public PlottableMap(int size) {
        super(size);
    }

    /**
     * Fill the map with the appropriate Plottable, for the given time span
     * specification
     * 
     * @param start the start time, in second
     * @param end the end time, in second
     * @param step the step, in second
     */
    public abstract void configure(long start, long end, long step);

    public void configure(ExtractInfo ei) {
        configure(ei.start.getTime() / 1000L, ei.end.getTime() / 1000L, ei.step);
    }
}
