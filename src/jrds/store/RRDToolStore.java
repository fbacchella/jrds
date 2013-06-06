package jrds.store;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import jrds.JrdsSample;
import jrds.Probe;
import jrds.factories.ProbeBean;

import org.apache.log4j.Level;
import org.rrd4j.core.jrrd.ConsolidationFunctionType;
import org.rrd4j.core.jrrd.DataChunk;
import org.rrd4j.core.jrrd.RRDatabase;
import org.rrd4j.data.Plottable;

@ProbeBean({"rrdfile"})
public class RRDToolStore extends AbstractStore<RRDatabase, DataChunk> {

    public RRDToolStore(Probe<?, ?> p) {
        super(p);
    }

    private File rrdpath;

    public Boolean configure(File rrdpath) {
        this.rrdpath = rrdpath;
        try {
            log(Level.TRACE, "rrd is %s", rrdpath.getCanonicalPath());
        } catch (IOException e) {
        }
        return rrdpath.canRead();
    }
    
    public void setRrdfile(String rrdpath) {
        this.rrdpath = new File(rrdpath);
    }
    
    public String getRrdfile() {
        return rrdpath.getPath();
    }

    /* (non-Javadoc)
     * @see jrds.Probe#checkStoreFile()
     */
    @Override
    public boolean checkStoreFile() {
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
        }
        return new Date(0);
    }

    @Override
    public void commit(JrdsSample sample) {
    }

    @Override
    public Map<String, Number> getLastValues() {
        return Collections.emptyMap();
    }

    @Override
    public Extractor<DataChunk> fetchData() {
        final RRDatabase db;
        try {
            db = new RRDatabase(rrdpath);
        } catch (IOException e1) {
            return null;
        }

        return new Extractor<DataChunk>() {

            @Override
            public String[] getNames() {
                return db.getDataSourcesName().toArray(new String[] {});
            }

            @Override
            public String[] getDsNames() {
                return db.getDataSourcesName().toArray(new String[] {});
            }

            @Override
            public double[][] getValues(ExtractInfo ei) {
                getSource(ei).getData();
                return null;
            }

            @Override
            protected DataChunk newPlottableSource(ExtractInfo ei) {
                try {
                    return db.getData(ConsolidationFunctionType.AVERAGE, ei.start, ei.end, ei.step);
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected Plottable newPlottable(DataChunk s, ExtractInfo ei) {
                return s.toPlottable(ei.ds);
            }

            @Override
            public long[] getTimestamps(ExtractInfo ei) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getColumnCount() {
                return db.getDataSourcesName().size();
            }

            @Override
            protected int getSignature(ExtractInfo ei) {
                return 0;
            }

            @Override
            public double getValue(ExtractInfo ei) {
                throw new UnsupportedOperationException();
            }

            @Override
            public double[] getSourceValues(ExtractInfo ei) {
                throw new UnsupportedOperationException();
            }
            
        };
    }

    @Override
    public RRDatabase getStoreObject() {
        try {
            return new RRDatabase(rrdpath);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void closeStoreObject(Object  object) {
        try {
            ((RRDatabase) object).close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
