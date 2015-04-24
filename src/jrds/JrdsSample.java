package jrds;

import java.util.Date;
import java.util.Map;

public interface JrdsSample extends Map<String, Number>{
    public Date getTime();
    public void setTime(Date time);
    public void put(Map.Entry<String, Double> e);
    public Probe<?, ?> getProbe();
}
