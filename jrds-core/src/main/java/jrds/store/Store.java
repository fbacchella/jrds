package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;

public interface Store<SO> {
    void commit(JrdsSample sample);
    Map<String, Number> getLastValues();
    boolean checkStoreFile(ArchivesSet archives);
    Date getLastUpdate();
    SO getStoreObject();
    void closeStoreObject(SO object);
    Extractor getExtractor();
    String getPath();
}
