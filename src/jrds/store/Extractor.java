package jrds.store;

import java.util.Collection;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public interface Extractor {

    public String[] getNames();
    public String[] getDsNames();
    public void fill(DataProcessor dp, ExtractInfo ei, Collection<String> sources);
    public void fill(RrdGraphDef gd, ExtractInfo ei, Collection<String> sources);
    public int getColumnCount();

}