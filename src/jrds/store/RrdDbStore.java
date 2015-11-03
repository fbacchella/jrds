package jrds.store;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;

import org.apache.log4j.Level;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.Archive;
import org.rrd4j.core.Datasource;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.Header;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;
import org.rrd4j.graph.RrdGraphDef;

public class RrdDbStore extends AbstractStore<RrdDb> {
    private final RrdDbStoreFactory factory;

    public RrdDbStore(Probe<?, ?> p, RrdDbStoreFactory factory) {
        super(p);
        this.factory = factory;
    }

    protected DsDef[] getDsDefs() {
        return p.getPd().getDsDefs();
    }

    public RrdDef getRrdDef(ArchivesSet archives) {
        RrdDef def = new RrdDef(getPath());
        def.setVersion(2);
        def.addDatasource(getDsDefs());
        def.addArchive(archives.getArchives());
        def.setStep(p.getStep());
        return def;
    }

    public String getPath() {
        String rrdName = p.getName().replaceAll("/","_");
        return p.getHost().getHostDir() +
                Util.getFileSeparator() + rrdName + ".rrd";
    }

    /**
     * Create the probe file
     * @throws IOException
     */
    protected void create(ArchivesSet archives) throws IOException {
        log(Level.INFO, "Need to create rrd");
        RrdDef def = getRrdDef(archives);
        RrdDb rrdDb = new RrdDb(def);
        rrdDb.close();
    }

    private void upgrade(ArchivesSet archives) {
        RrdDb rrdSource = null;
        try {
            log(Level.WARN,"Definition is changed, the store needs to be upgraded");
            File source = new File(getPath());
            rrdSource = new RrdDb(source.getCanonicalPath());

            RrdDef rrdDef = getRrdDef(archives);
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

    public boolean checkStoreFile(ArchivesSet archives) {
        File rrdFile = new File(getPath());

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
                rrdDb = new RrdDb(getPath());
                //old definition
                RrdDef tmpdef = rrdDb.getRrdDef();
                Date startTime = new Date();
                tmpdef.setStartTime(startTime);
                String oldDef = tmpdef.dump();
                long oldstep = tmpdef.getStep();
                log(Level.TRACE, "Definition found: %s\n", oldDef);

                //new definition
                tmpdef = getRrdDef(archives);
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
                    upgrade(archives);
                    rrdDb = new RrdDb(getPath());
                }
                log(Level.TRACE, "******");
            } else
                create(archives);
            retValue = true;
        } catch (Exception e) {
            log(Level.ERROR, e, "Store %s unusable: %s", getPath(), e);
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
            rrdDb = factory.getRrd(getPath());
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
    public AbstractExtractor<FetchData> getExtractor() {
        final RrdDb rrdDb;
        try {
            rrdDb = factory.getRrd(getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to access rrd file  " + getPath(), e);
        }

        return new jrds.store.AbstractExtractor<FetchData>() {
            boolean released = false;
            
            public void release() {
                factory.releaseRrd(rrdDb);
                released = true;
            }
            
            /* (non-Javadoc)
             * @see java.lang.Object#finalize()
             */
            @Override
            protected void finalize() throws Throwable {
                if(! released) {
                    log(Level.WARN,"%s was not release properly", rrdDb.getCanonicalPath());
                    factory.releaseRrd(rrdDb);
                    released = true;
                }
                super.finalize();
            }

            @Override
            public int getColumnCount() {
                return rrdDb.getDsCount();
            }

            @Override
            public void fill(RrdGraphDef gd, ExtractInfo ei) {
                for(Map.Entry<String, String> e: sources.entrySet()) {
                    gd.datasource(e.getKey(), RrdDbStore.this.getPath(), e.getValue(), ei.cf);
                }
            }

            @Override
            public void fill(DataProcessor dp, ExtractInfo ei) {
                for(Map.Entry<String, String> e: sources.entrySet()) {
                    dp.addDatasource(e.getKey(), RrdDbStore.this.getPath(), e.getValue(), ei.cf);
                }
            }

            @Override
            public String getPath() {
                return RrdDbStore.this.getPath();
            }

        };
    }

    public Map<String, Number> getLastValues() {
        Map<String, Number> retValues = new HashMap<String, Number>();
        RrdDb rrdDb = null;
        try {
            rrdDb = factory.getRrd(getPath());
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
            rrdDb = factory.getRrd(getPath());
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
            return factory.getRrd(getPath());
        } catch (IOException e) {
            log(Level.ERROR, e, "Failed to access rrd file %s: %s ", getPath(), e.getMessage());
            return null;
        }
    }

    @Override
    public void closeStoreObject(Object rrdDb) {
        if(rrdDb != null)
            factory.releaseRrd((RrdDb) rrdDb);
    }

}
