package jrds.mockobjects;

import java.util.Date;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;
import jrds.store.AbstractStore;
import jrds.store.AbstractStoreFactory;
import jrds.store.Extractor;

public class EmptyStoreFactory extends AbstractStoreFactory<EmptyStoreFactory.EmptyStore> {
    public class EmptyStore extends AbstractStore<String> {

        public EmptyStore(Probe<?, ?> p) {
            super(p);
        }

        @Override
        public Extractor getExtractor() {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public void commit(JrdsSample sample) {

        }

        @Override
        public Map<String, Number> getLastValues() {
            return null;
        }

        @Override
        public boolean checkStoreFile(ArchivesSet archives) {
            return false;
        }

        @Override
        public Date getLastUpdate() {
            return null;
        }

        @Override
        public String getStoreObject() {
            return null;
        }

        @Override
        public void closeStoreObject(Object object) {

        }

    }
    @Override
    public EmptyStore create(Probe<?, ?> p) {
        return null;
    }

}
