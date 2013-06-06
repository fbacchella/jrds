package jrds.store;

import org.rrd4j.data.Plottable;

public interface Extractor {

    public  String[] getNames();
    public  String[] getDsNames();
    public  double[][] getValues(ExtractInfo ei);
    public  double[] getSourceValues(ExtractInfo ei);
    public  Plottable getPlottable(ExtractInfo ei);
    public  long[] getTimestamps(ExtractInfo ei);
    public  int getColumnCount();
    public  double getValue(ExtractInfo ei);
    

}