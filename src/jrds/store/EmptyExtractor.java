package jrds.store;

import java.util.Collection;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public class EmptyExtractor extends AbstractExtractor<Object> {

    @Override
    public String[] getNames() {
        return new String[] {};
    }

    @Override
    public String[] getDsNames() {
        return new String[] {};
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
    public void fill(RrdGraphDef gd, ExtractInfo ei, Collection<String> sources) {
    }

    @Override
    public void fill(DataProcessor dp, ExtractInfo ei, Collection<String> sources) {

    }

}
