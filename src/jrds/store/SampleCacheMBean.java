package jrds.store;

import java.util.Map;

public interface SampleCacheMBean {
    Map<String, Number> getValues(String host, String probe);
}
