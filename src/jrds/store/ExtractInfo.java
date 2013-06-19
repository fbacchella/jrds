package jrds.store;

import java.util.Date;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;

public class ExtractInfo {
    final Date start;
    final Date end;
    final Long step;
    final String ds;
    final ConsolFun cf;

    public static final ExtractInfo get() {
        return new ExtractInfo();
    }

    private ExtractInfo() {
        start = new Date();
        end = new Date();
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

    public final ExtractInfo make(Object source) {
        return new ExtractInfo(this.start, this.end, this.step, this.ds, this.cf);
    }

    public final ExtractInfo make(Date start, Date end) {
        return new ExtractInfo(start, end, this.step, this.ds, this.cf);
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

}
