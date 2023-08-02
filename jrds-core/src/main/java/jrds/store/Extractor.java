package jrds.store;

import java.io.Closeable;

import org.rrd4j.core.DataHolder;

public interface Extractor extends Closeable {

    void release();
    String[] getNames();
    String[] getDsNames();
    void fill(DataHolder dp, ExtractInfo ei);
    int getColumnCount();
    void addSource(String name, String dsName);
    String getPath();
    default void close() { release(); };
}
