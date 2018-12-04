package jrds.store;

import java.io.Closeable;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public interface Extractor extends Closeable {

    public abstract void release();
    public String[] getNames();
    public String[] getDsNames();
    public void fill(DataProcessor dp, ExtractInfo ei);
    public void fill(RrdGraphDef gd, ExtractInfo ei);
    public int getColumnCount();
    public void addSource(String name, String dsName);
    public String getPath();
    public default void close() { release(); }; 
}
