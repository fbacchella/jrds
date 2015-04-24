package jrds.store;

import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;

public interface Store {
    public void commit(JrdsSample sample);
    public Map<String, Number> getLastValues();
    public boolean checkStoreFile(ArchivesSet archives);
    public Date getLastUpdate();
    public Object getStoreObject();
    public void closeStoreObject(Object object);
    public Extractor getExtractor();
    public String getPath();

}
