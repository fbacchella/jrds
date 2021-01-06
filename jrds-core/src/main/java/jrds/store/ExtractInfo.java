package jrds.store;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.DataProcessor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.AccessLevel;

@Builder @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExtractInfo {

    public static class ExtractInfoBuilder {
        private ExtractInfoBuilder() {
            start = Instant.now();
            end = Instant.now();
            step = 0;
            ds = "";
            cf = ConsolFun.AVERAGE;
        }
        public ExtractInfoBuilder interval(Date start, Date end) {
            this.start = Instant.ofEpochMilli(start.getTime());
            this.end = Instant.ofEpochMilli(end.getTime());
            return this;
        }
        public ExtractInfoBuilder interval(Instant start, Instant end) {
            this.start = start;
            this.end = end;
            return this;
        }
        public ExtractInfoBuilder step(int step) {
            this.step = step;
            return this;
        }
        public ExtractInfoBuilder step(long step) {
            this.step = step;
            return this;
        }
    }

    public final Instant start;
    public final Instant end;
    public final long step;
    public final String ds;
    public final ConsolFun cf;

    public final DataProcessor getDataProcessor(Extractor ex) {
        DataProcessor dp = new DataProcessor(start.getEpochSecond(), end.getEpochSecond());
        ex.fill(dp, this);
        try {
            dp.processData();
        } catch (IOException e) {
            throw new RuntimeException("Failed to access rrd file  " + ex.getPath(), e);
        }
        return dp;
    }
    
    public final DataProcessor getDataProcessor() {
        DataProcessor dp = new DataProcessor(start.getEpochSecond(), end.getEpochSecond());
        if (step != 0) {
            dp.setStep(step);
        }
        return dp;
    }

    public static ExtractInfo of(Date start, Date end) {
        return ExtractInfo.builder().interval(start, end).build();
    }

}
