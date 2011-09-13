package jrds;

import java.util.HashMap;

import org.rrd4j.data.Plottable;

public abstract class ProxyPlottableMap extends HashMap<String, ProxyPlottableMap.ProxyPlottable>{
	public static class ProxyPlottable extends Plottable {
		Plottable real = new Plottable() {};
		@Override
		public double getValue(long timestamp) {
			return real.getValue(timestamp);
		}
		public void setReal(Plottable real) {
			this.real = real;
		}
	}
	public abstract void configure(long start, long end, long step);
}
