package jrds;

import java.util.Date;
import java.util.Map;

public interface JrdsSample extends Map<String, Number> {
    Date getTime();
    void setTime(Date time);
    void put(Map.Entry<String, Double> e);
    Probe<?, ?> getProbe();
}
