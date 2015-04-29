package jrds.store;

import java.io.IOException;
import java.util.Date;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;

public class ExtractInfo {
    private final class ImmutableDate extends Date {
        ImmutableDate() {
            super();
        }
        ImmutableDate(Date date) {
            super(date.getTime());
        }
        @Override
        public void setTime(long time) {
            throw new UnsupportedOperationException("read only date");
        }
    };


    public final Date start;
    public final Date end;
    public final Long step;
    public final String ds;
    public final ConsolFun cf;

    public static final ExtractInfo get() {
        return new ExtractInfo();
    }

    private ExtractInfo() {
        start = new ImmutableDate();
        end = new ImmutableDate();
        step = new Long(0);
        ds = "";
        cf = ConsolFun.AVERAGE;
    }

    private ExtractInfo(Date start, Date end, long step,
            String ds, ConsolFun cf) {
        super();
        this.start = start;
        this.end = end;
        this.step = step;
        this.ds = ds;
        this.cf = cf;
    }

    public final ExtractInfo make(Date start, Date end) {
        return new ExtractInfo(new ImmutableDate(start), new ImmutableDate(end), this.step, this.ds, this.cf);
    }

    public final ExtractInfo make(long step) {
        return new ExtractInfo(this.start, this.end, step, this.ds, this.cf);
    }

    public final ExtractInfo make(String ds) {
        return new ExtractInfo(this.start, this.end, this.step, ds, this.cf);
    }

    public final ExtractInfo make(ConsolFun cf) {
        return new ExtractInfo(this.start, this.end, this.step, this.ds, cf);
    }

    public final DataProcessor getDataProcessor() {
        return new DataProcessor(start, end);
    }

    public final DataProcessor getDataProcessor(Extractor ex) {
        DataProcessor dp = new DataProcessor(start, end);
        ex.fill(dp, this);
        try {
            dp.processData();
        } catch (IOException e) {
            throw new RuntimeException("Failed to access rrd file  " + ex.getPath(), e);
        }
        return dp;
    }

}
