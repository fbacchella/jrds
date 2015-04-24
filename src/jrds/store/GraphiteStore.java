package jrds.store;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Level;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;

public class GraphiteStore extends AbstractStore<GraphiteConnection> {
    GraphiteConnection cnx;
    Date lastUpdate = new Date(0);

    public GraphiteStore(Probe<?, ?> p, GraphiteConnection cnx) {
        super(p);
        this.cnx = cnx;
    }

    @Override
    public Extractor getExtractor() {
        return new EmptyExtractor();
    }

    @Override
    public String getPath() {
        return cnx.getPrefix(p);
    }

    @Override
    public void commit(JrdsSample sample) {
        try {
            cnx.send(p, sample);
            lastUpdate = sample.getTime();
        } catch (Exception e) {
            log(Level.ERROR, e, "Failed to send sample to graphite server: %s", e.getMessage());
        }
    }

    @Override
    public Map<String, Number> getLastValues() {
        return Collections.emptyMap();
    }

    @Override
    public boolean checkStoreFile(ArchivesSet archives) {
        try {
            cnx.ensureGraphiteConnection();
        } catch (IOException e) {
        }
        return true;
    }

    @Override
    public Date getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public GraphiteConnection getStoreObject() {
        return cnx;
    }

    @Override
    public void closeStoreObject(Object object) {

    }

}
