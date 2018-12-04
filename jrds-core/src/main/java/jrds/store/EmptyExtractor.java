package jrds.store;

import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public class EmptyExtractor extends AbstractExtractor<Object> {

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    public void fill(RrdGraphDef gd, ExtractInfo ei) {
    }

    @Override
    public void fill(DataProcessor dp, ExtractInfo ei) {

    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public void release() {
    }

}
