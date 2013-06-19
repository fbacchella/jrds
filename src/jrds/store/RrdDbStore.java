package jrds.store;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jrds.JrdsSample;
import jrds.Probe;

import org.apache.log4j.Level;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.Archive;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.Header;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public class RrdDbStore extends AbstractStore<RrdDb, FetchData> {
    private final RrdDbStoreFactory factory;

    private static final ArcDef[] DEFAULTARC = {
        new ArcDef(ConsolFun.AVERAGE, 0.5, 1, 12 * 24 * 30 * 3),
        new ArcDef(ConsolFun.AVERAGE, 0.5, 12, 24 * 365), 
        new ArcDef(ConsolFun.AVERAGE, 0.5, 288, 365 * 2)
    };

    public RrdDbStore(Probe<?, ?> p, RrdDbStoreFactory factory) {
        super(p);
        this.factory = factory;
    }

    protected DsDef[] getDsDefs() {
        return p.getPd().getDsDefs();
    }

    public RrdDef getRrdDef() {
        RrdDef def = new RrdDef(getRrdName());
        def.setVersion(2);
        def.addArchive(DEFAULTARC);
        def.addDatasource(getDsDefs());
        def.setStep(p.getStep());
        return def;
    }

    public String getRrdName() {
        String rrdName = p.getName().replaceAll("/","_");
        return p.getHost().getHostDir() +
                Util.getFileSeparator() + rrdName + ".rrd";
    }

    /**
     * Create the probe file
     * @throws IOException
     */
    protected void create() throws IOException {
        log(Level.INFO, "Need to create rrd");
        RrdDef def = getRrdDef();
        RrdDb rrdDb = new RrdDb(def);
        rrdDb.close();
    }

    private void upgrade() {
        RrdDb rrdSource = null;
        try {
            log(Level.WARN,"Definition is changed, the store needs to be upgraded");
            File source = new File(getRrdName());
            rrdSource = new RrdDb(source.getCanonicalPath());

            RrdDef rrdDef = getRrdDef();
            File dest = File.createTempFile("JRDS_", ".tmp", source.getParentFile());
            rrdDef.setPath(dest.getCanonicalPath());
            RrdDb rrdDest = new RrdDb(rrdDef);

            log(Level.DEBUG, "updating %s to %s",source, dest);

            Set<String> badDs = new HashSet<String>();
            Header header = rrdSource.getHeader();
            int dsCount = header.getDsCount();;
            header.copyStateTo(rrdDest.getHeader());
            for (int i = 0; i < dsCount; i++) {
                Datasource srcDs = rrdSource.getDatasource(i);
                String dsName = srcDs.getName();
                Datasource dstDS = rrdDest.getDatasource(dsName);
                if (dstDS != null ) {
                    try {
                        srcDs.copyStateTo(dstDS);
                        log(Level.TRACE, "Update %s", dsName);
                    } catch (RuntimeException e) {
                        badDs.add(dsName);
                        log(Level.ERROR, e, "Datasource %s can't be upgraded: %s", dsName,  e.getMessage());
                    }
                }
            }
            int robinMigrated = 0;
            for (int i = 0; i < rrdSource.getArcCount(); i++) {
                Archive srcArchive = rrdSource.getArchive(i);
                ConsolFun consolFun = srcArchive.getConsolFun();
                int steps = srcArchive.getSteps();
                Archive dstArchive = rrdDest.getArchive(consolFun, steps);
                if (dstArchive != null) {
                    if ( dstArchive.getConsolFun().equals(srcArchive.getConsolFun())  &&
                            dstArchive.getSteps() == srcArchive.getSteps() ) {
                        for (int k = 0; k < dsCount; k++) {
                            Datasource srcDs = rrdSource.getDatasource(k);
                            String dsName = srcDs.getName();
                            try {
                                int j = rrdDest.getDsIndex(dsName);
                                if (j >= 0 && ! badDs.contains(dsName)) {
                                    log(Level.TRACE, "Upgrade of %s from %s", dsName, srcArchive);
                                    srcArchive.getArcState(k).copyStateTo(dstArchive.getArcState(j));
                                    srcArchive.getRobin(k).copyStateTo(dstArchive.getRobin(j));
                                    robinMigrated++;
                                }
                            }
                            catch (IllegalArgumentException e) {
                                log(Level.TRACE, "Datastore %s removed", dsName);
                            }

                        }
                        log(Level.TRACE, "Update %s", srcArchive);
                    }
                }
            }
            log(Level.DEBUG, "Robin migrated: %s", robinMigrated);

            rrdDest.close();
            rrdSource.close();
            log(Level.DEBUG, "Size difference : %d", (dest.length() - source.length()));
            copyFile(dest.getCanonicalPath(), source.getCanonicalPath());
        } catch (IOException e) {
            log(Level.ERROR, e, "Upgrade failed: %s", e);
        }
        finally {
            if(rrdSource != null)
                try {
                    rrdSource.close();
                } catch (IOException e) {
                }
        }
    }

    private static void copyFile(String sourcePath, String destPath)
            throws IOException {
        File source = new File(sourcePath);
        File dest = new File(destPath);
        File destOld = new File(destPath + ".old");
        if (!dest.renameTo(destOld)) {
            throw new IOException("Could not rename file " + destPath + " from " + destOld);
        }
        if (!source.renameTo(dest)) {
            throw new IOException("Could not rename file " + destPath + " from " + sourcePath);
        }
        deleteFile(destOld);
    }

    private static void deleteFile(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not delete file: " + file.getCanonicalPath());
        }
    }

    public boolean checkStoreFile() {
        File rrdFile = new File(getRrdName());

        File rrdDir = p.getHost().getHostDir();
        if (!rrdDir.isDirectory()) {
            if( ! rrdDir.mkdir()) {
                try {
                    log(Level.ERROR, "prode dir %s creation failed ", rrdDir.getCanonicalPath());
                } catch (IOException e) {
                }
                return false;
            }
        }

        boolean retValue = false;
        RrdDb rrdDb = null;
        try {
            if ( rrdFile.isFile() ) {
                rrdDb = new RrdDb(getRrdName());
                //old definition
                RrdDef tmpdef = rrdDb.getRrdDef();
                Date startTime = new Date();
                tmpdef.setStartTime(startTime);
                String oldDef = tmpdef.dump();
                long oldstep = tmpdef.getStep();
                log(Level.TRACE, "Definition found: %s\n", oldDef);

                //new definition
                tmpdef = getRrdDef();
                tmpdef.setStartTime(startTime);
                String newDef = tmpdef.dump();
                long newstep = tmpdef.getStep();

                if(newstep != oldstep ) {
                    log(Level.ERROR, "step changed, you're in trouble" );
                    return false;
                }
                else if(! newDef.equals(oldDef)) {

                    rrdDb.close();
                    rrdDb = null;
                    upgrade();
                    rrdDb = new RrdDb(getRrdName());
                }
                log(Level.TRACE, "******");
            } else
                create();
            retValue = true;
        } catch (Exception e) {
            log(Level.ERROR, e, "Store %s unusable: %s", getRrdName(), e);
        }
        finally {
            if(rrdDb != null)
                try {
                    rrdDb.close();
                } catch (IOException e) {
                }

        }
        return retValue;
    }

    /**
     * Return the date of the last update of the rrd backend
     * @return The date
     */
    public Date getLastUpdate() {
        Date lastUpdate = null;
        RrdDb rrdDb = null;
        try {
            rrdDb = factory.getRrd(getRrdName());
            lastUpdate = Util.getDate(rrdDb.getLastUpdateTime());
        } catch (Exception e) {
            throw new RuntimeException("Unable to get last update date for " + p.getQualifiedName(), e);
        }
        finally {
            if(rrdDb != null)
                factory.releaseRrd(rrdDb);
        }
        return lastUpdate;
    }

    @Override
    public AbstractExtractor<FetchData> fetchData() {
        try {
            final RrdDb rrdDb = factory.getRrd(getRrdName());
            return new jrds.store.AbstractExtractor<FetchData>() {
                /* (non-Javadoc)
                 * @see java.lang.Object#finalize()
                 */
                @Override
                protected void finalize() throws Throwable {
                    super.finalize();
                    factory.releaseRrd(rrdDb);
                }

                @Override
                public String[] getNames() {
                    try {
                        return rrdDb.getDsNames();
                    } catch (IOException e) {
                        return null;
                    }
                }

                @Override
                public String[] getDsNames() {
                    try {
                        return rrdDb.getDsNames();
                    } catch (IOException e) {
                        return null;
                    }
                }

                @Override
                public int getColumnCount() {
                    return rrdDb.getDsCount();
                }

                @Override
                protected int getSignature(ExtractInfo ei) {
                    return ExtractInfo.get().make(ei.start, ei.end).make(ei.step).make(ei.cf).hashCode();
                }

                @Override
                public void fill(RrdGraphDef gd, ExtractInfo ei,
                        Collection<String> sources) {
                    try {
                        FetchRequest fr = rrdDb.createFetchRequest(ei.cf, ei.start.getTime() / 1000, ei.end.getTime() / 1000, 1);
                        FetchData fd = fr.fetchData();
                        for(String source: sources) {
                            gd.datasource(source, fd);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to access rrd file  " + getRrdName(), e);
                    }
                }

                @Override
                public void fill(DataProcessor dp, ExtractInfo ei,
                        Collection<String> sources) {
                    try {
                        FetchRequest fr = rrdDb.createFetchRequest(ei.cf, ei.start.getTime() / 1000, ei.end.getTime() / 1000, 1);
                        FetchData fd = fr.fetchData();
                        for(String source: sources) {
                            dp.addDatasource(source, fd);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to access rrd file  " + getRrdName(), e);
                    }
                }

            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to access rrd file  " + getRrdName(), e);
        }
    }

    public Map<String, Number> getLastValues() {
        Map<String, Number> retValues = new HashMap<String, Number>();
        RrdDb rrdDb = null;
        try {
            rrdDb = factory.getRrd(getRrdName());
            String[] dsNames = rrdDb.getDsNames();
            for(int i = 0; i < dsNames.length ; i ++) {
                retValues.put(dsNames[i], rrdDb.getDatasource(i).getLastValue());
            }
        } catch (Exception e) {
            log(Level.ERROR, e, "Unable to get last values: %s", e.getMessage());
        }
        finally {
            if(rrdDb != null)
                factory.releaseRrd(rrdDb);
        }
        return retValues;
    }

    public void commit(JrdsSample sample) {
        RrdDb rrdDb = null;
        try {
            rrdDb = factory.getRrd(getRrdName());
            Sample onesample = rrdDb.createSample(sample.getTime().getTime() / 1000);
            for(Map.Entry<String, Number> e: sample.entrySet()) {
                onesample.setValue(e.getKey(), e.getValue().doubleValue());
            }
            if(p.getNamedLogger().isDebugEnabled())
                log(Level.DEBUG, "%s", onesample.dump());
            onesample.update();
        } catch (IOException e) {
            log(Level.ERROR, e, "Error while collecting: %s", e.getMessage());
        }            
        finally  {
            if(rrdDb != null)
                factory.releaseRrd(rrdDb);
        }
    }

    @Override
    public RrdDb getStoreObject() {
        try {
            return factory.getRrd(getRrdName());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void closeStoreObject(Object rrdDb) {
        if(rrdDb != null)
            factory.releaseRrd((RrdDb) rrdDb);
    }

}
