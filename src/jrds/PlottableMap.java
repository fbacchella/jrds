package jrds;

import java.util.HashMap;

import jrds.store.ExtractInfo;

import org.rrd4j.data.Plottable;

public abstract class PlottableMap extends HashMap<String, Plottable>{
    public static class ProxyPlottable extends Plottable {
        Plottable real = new Plottable() {
            @Override
            public double getValue(long timestamp) {
                return Double.NaN;
            }
        };
        @Override
        public double getValue(long timestamp) {
            return real.getValue(timestamp);
        }
        public void setReal(Plottable real) {
            this.real = real;
        }
    };

    public static final PlottableMap Empty = new PlottableMap(0) {
        @Override
        public void configure(long start, long end, long step) {
        }
        @Override
        public Plottable put(String key, Plottable value) {
            throw new UnsupportedOperationException("read only empty map");
        }
        @Override
        public Plottable get(Object key) {
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
     * Fill the map with the appropriate Plottable, for the given time span specification
     * @param start the start time, in second
     * @param end the end time, in second
     * @param step the step, in second
     */
    public abstract void configure(long start, long end, long step);

    public void configure(ExtractInfo ei) {
        configure(ei.start.getTime() / 1000L, ei.end.getTime() / 1000L, ei.step);
    }
}
