package jrds.store;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;
import jrds.factories.ProbeBean;

import org.apache.log4j.Level;
import org.rrd4j.core.jrrd.ConsolidationFunctionType;
import org.rrd4j.core.jrrd.DataChunk;
import org.rrd4j.core.jrrd.DataSource;
import org.rrd4j.core.jrrd.RRDatabase;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

@ProbeBean({"rrdfile"})
public class RRDToolStore extends AbstractStore<RRDatabase> {

    public RRDToolStore(Probe<?, ?> p) {
        super(p);
    }

    private File rrdpath;

    public Boolean configure(File rrdpath) {
        this.rrdpath = rrdpath;
        return rrdpath.canRead();
    }

    public void setRrdfile(String rrdpath) {
        this.rrdpath = new File(rrdpath);
    }

    public String getRrdfile() {
        return getPath();
    }

    public String getPath() {
        return rrdpath.getPath();
    }

    /* (non-Javadoc)
     * @see jrds.Probe#checkStoreFile()
     */
    @Override
    public boolean checkStoreFile(ArchivesSet archives) {
        return rrdpath.canRead();
    }

    /* (non-Javadoc)
     * @see jrds.Probe#getLastUpdate()
     */
    @Override
    public Date getLastUpdate() {
        try {
            RRDatabase db = new RRDatabase(rrdpath);
            return db.getLastUpdate();
        } catch (IOException e) {
            log(Level.ERROR, e, "invalid rrd file %s: %s", rrdpath, e.getMessage());
            return new Date(0);
        }
    }

    @Override
    public void commit(JrdsSample sample) {
    }

    @Override
    public Map<String, Number> getLastValues() {
        RRDatabase db;
        try {
            db = new RRDatabase(rrdpath);
            Map<String, Number> values = new HashMap<String, Number>(db.getDataSourcesName().size());
            for(int i = db.getDataSourcesName().size() - 1; i>=0; i--) {
                DataSource source = db.getDataSource(i);
                values.put(source.getName(), source.getPDPStatusBlock().getValue());
            }
            return values;
        } catch (IOException e) {
            log(Level.ERROR, e, "invalid rrd file %s: %s", rrdpath, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public AbstractExtractor<DataChunk> getExtractor() {
        final RRDatabase db;
        try {
            db = new RRDatabase(rrdpath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to access rrd file  " + getPath(), e);
        }

        return new AbstractExtractor<DataChunk>() {

            @Override
            protected void finalize() throws Throwable {
                super.finalize();
                db.close();
            }

            @Override
            public int getColumnCount() {
                return db.getDataSourcesName().size();
            }

            @Override
            public void fill(RrdGraphDef gd, ExtractInfo ei) {
                try {
                    DataChunk dc = db.getData(ConsolidationFunctionType.AVERAGE, ei.start, ei.end, ei.step);
                    for(Map.Entry<String, String> e: sources.entrySet()) {
                        gd.datasource(e.getKey(), dc.toPlottable(e.getValue()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to access rrd file  " + db.toString(), e);
                }
            }

            @Override
            public void fill(DataProcessor dp, ExtractInfo ei) {
                try {
                    DataChunk dc = db.getData(ConsolidationFunctionType.AVERAGE, ei.start, ei.end, ei.step);
                    for(Map.Entry<String, String> e: sources.entrySet()) {
                        dp.addDatasource(e.getKey(), dc.toPlottable(e.getValue()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to access rrd file  " + RRDToolStore.this.getPath(), e);
                }
            }

            @Override
            public String getPath() {
                return RRDToolStore.this.getPath();
            }

            @Override
            public void release() {
                try {
                    db.close();
                } catch (IOException e) {
                    log(Level.ERROR," failed to close %s: s%", db, e);
                }
            }

        };
    }

    @Override
    public RRDatabase getStoreObject() {
        try {
            return new RRDatabase(rrdpath);
        } catch (IOException e) {
            log(Level.ERROR, e, "invalid rrd file %s: %s", rrdpath, e.getMessage());
            return null;
        }
    }

    @Override
    public void closeStoreObject(Object  object) {
        try {
            ((RRDatabase) object).close();
        } catch (IOException e) {
            log(Level.ERROR, e, "invalid rrd file %s: %s", rrdpath, e.getMessage());
        }
    }

}
