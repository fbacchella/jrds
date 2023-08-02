package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;

public interface Store {
    void commit(JrdsSample sample);
    Map<String, Number> getLastValues();
    boolean checkStoreFile(ArchivesSet archives);
    Date getLastUpdate();
    Object getStoreObject();
    void closeStoreObject(Object object);
    Extractor getExtractor();
    String getPath();

}
