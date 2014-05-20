package jrds.jmx;

import java.util.Map;

public interface ManagementMBean {
    public void reload();
    public int getHostsCount();
    public int getProbesCount();
    public int getGeneration();
    public Map<String, Number> getLastValues(String host, String probeName);
}
