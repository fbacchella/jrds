package jrds.store;

import org.rrd4j.core.DataHolder;

public class EmptyExtractor extends AbstractExtractor<Object> {

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    public void fill(DataHolder gd, ExtractInfo ei) {
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public void release() {
    }

}
