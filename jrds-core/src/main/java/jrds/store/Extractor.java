package jrds.store;

import java.io.Closeable;

import org.rrd4j.core.DataHolder;

public interface Extractor extends Closeable {

    public abstract void release();
    public String[] getNames();
    public String[] getDsNames();
    public void fill(DataHolder dp, ExtractInfo ei);
    public int getColumnCount();
    public void addSource(String name, String dsName);
    public String getPath();
    public default void close() { release(); }; 
}
