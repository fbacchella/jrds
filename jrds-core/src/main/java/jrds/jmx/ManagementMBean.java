package jrds.jmx;

import java.util.Map;

public interface ManagementMBean {
    void reload();
    int getHostsCount();
    int getProbesCount();
    int getGeneration();
    Map<String, Number> getLastValues(String host, String probeName);
}
