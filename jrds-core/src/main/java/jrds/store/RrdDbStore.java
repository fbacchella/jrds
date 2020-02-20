package jrds.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.slf4j.event.Level;

import jrds.ArchivesSet;
import jrds.JrdsSample;
import jrds.Probe;
import lombok.Getter;

public class RrdDbStore extends AbstractStore<RrdDb> {

    private class RrdDbExtractor extends AbstractExtractor<FetchData> {
        @Getter
        private final int columnCount;

        RrdDbExtractor(RrdDb rrdDb) {
            this.columnCount = rrdDb.getDsCount();
        }

        @Override
        public void release() {
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
    }

    private final RrdDbStoreFactory factory;

    public RrdDbStore(Probe<?, ?> p, RrdDbStoreFactory factory) {
        super(p);
        this.factory = factory;
    }

    protected DsDef[] getDsDefs() {
        return p.getPd().getDsDefs(p.getRequiredUptime());
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
        try {
            return resolvePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Can't resolve probe path", e);
        }
    }

    private Path resolvePath() throws IOException {
        String rrdName = p.getName().replaceAll("/", "_");
        return Paths.get(p.getHost().getHostDir().getPath(), rrdName + ".rrd").normalize();
    }

    /**
     * Create the probe file
     * 
     * @throws IOException
     */
    protected void create(ArchivesSet archives) throws IOException {
        log(Level.INFO, "Need to create rrd");
        RrdDef def = getRrdDef(archives);
        RrdDb.getBuilder().setRrdDef(def).build().close();
    }

    private void upgrade(ArchivesSet archives) {
        try {
            log(Level.WARN, "Definition is changed, the store needs to be upgraded");
            File source = new File(getPath());
            RrdDef rrdDef = getRrdDef(archives);
            File dest = File.createTempFile("JRDS_", ".tmp", source.getParentFile());
            rrdDef.setPath(dest.getCanonicalPath());
            try (RrdDb rrdSource = RrdDb.getBuilder().setPath(source.getCanonicalPath()).build();
                 RrdDb rrdDest = RrdDb.getBuilder().setRrdDef(rrdDef).build()) {
                log(Level.DEBUG, "Updating %s to %s", source, dest);
                Set<String> badDs = new HashSet<String>();
                Header header = rrdSource.getHeader();
                int dsCount = header.getDsCount();
                header.copyStateTo(rrdDest.getHeader());
                for(int i = 0; i < dsCount; i++) {
                    Datasource srcDs = rrdSource.getDatasource(i);
                    String dsName = srcDs.getName();
                    Datasource dstDS = rrdDest.getDatasource(dsName);
                    if(dstDS != null) {
                        try {
                            srcDs.copyStateTo(dstDS);
                            log(Level.TRACE, "Update %s", dsName);
                        } catch (RuntimeException e) {
                            badDs.add(dsName);
                            log(Level.ERROR, e, "Datasource %s can't be upgraded: %s", dsName, e);
                        }
                    }
                }
                int robinMigrated = 0;
                for(int i = 0; i < rrdSource.getArcCount(); i++) {
                    Archive srcArchive = rrdSource.getArchive(i);
                    ConsolFun consolFun = srcArchive.getConsolFun();
                    int steps = srcArchive.getSteps();
                    Archive dstArchive = rrdDest.getArchive(consolFun, steps);
                    if(dstArchive != null) {
                        if(dstArchive.getConsolFun().equals(srcArchive.getConsolFun()) && dstArchive.getSteps() == srcArchive.getSteps()) {
                            for(int k = 0; k < dsCount; k++) {
                                Datasource srcDs = rrdSource.getDatasource(k);
                                String dsName = srcDs.getName();
                                try {
                                    int j = rrdDest.getDsIndex(dsName);
                                    if(j >= 0 && !badDs.contains(dsName)) {
                                        log(Level.TRACE, "Upgrade of %s from %s", dsName, srcArchive);
                                        srcArchive.getArcState(k).copyStateTo(dstArchive.getArcState(j));
                                        srcArchive.getRobin(k).copyStateTo(dstArchive.getRobin(j));
                                        robinMigrated++;
                                    }
                                } catch (IllegalArgumentException e) {
                                    log(Level.TRACE, e, "Datastore %s removed: %s", dsName, e);
                                }
                            }
                            log(Level.TRACE, "Update %s", srcArchive);
                        }
                    }
                }
                log(Level.DEBUG, "Robin migrated: %s", robinMigrated);
                rrdSource.close();
            }
            log(Level.DEBUG, "Size difference : %d", (dest.length() - source.length()));
            copyFile(dest.getCanonicalPath(), source.getCanonicalPath());
        } catch (IOException e) {
            log(Level.ERROR, e, "Upgrade failed: %s", e);
        }
    }

    private static void copyFile(String sourcePath, String destPath) throws IOException {
        File source = new File(sourcePath);
        File dest = new File(destPath);
        File destOld = new File(destPath + ".old");
        if(!dest.renameTo(destOld)) {
            throw new IOException("Could not rename file " + destPath + " from " + destOld);
        }
        if(!source.renameTo(dest)) {
            throw new IOException("Could not rename file " + destPath + " from " + sourcePath);
        }
        deleteFile(destOld);
    }

    private static void deleteFile(File file) throws IOException {
        if(file.exists() && !file.delete()) {
            throw new IOException("Could not delete file: " + file.getCanonicalPath());
        }
    }

    public boolean checkStoreFile(ArchivesSet archives) {
        File rrdDir = p.getHost().getHostDir();
        if (!rrdDir.isDirectory()) {
            if (!rrdDir.mkdir()) {
                try {
                    log(Level.ERROR, "prode dir %s creation failed ", rrdDir.getCanonicalPath());
                } catch (IOException e) {
                }
                return false;
            }
        }
        boolean retValue = false;
        try {
            Path rrdPath = resolvePath();
            if (Files.exists(rrdPath)) {
                Date startTime = new Date();
                rrdPath = rrdPath.toRealPath(LinkOption.NOFOLLOW_LINKS);

                // old definition
                RrdDef oldDef;
                try (RrdDb rrdDb = RrdDb.of(rrdPath.toString())) {
                    oldDef = rrdDb.getRrdDef();
                }
                oldDef.setStartTime(startTime);
                oldDef.setPath(rrdPath.toString());
                String oldDefDump = oldDef.dump();
                long oldstep = oldDef.getStep();
                log(Level.TRACE, "Definition found: %s\n", oldDefDump);
                // new definition
                RrdDef newDef = getRrdDef(archives);
                newDef.setStartTime(startTime);
                newDef.setPath(rrdPath.toString());
                String newDefDump = newDef.dump();
                long newstep = oldDef.getStep();
                if(newstep != oldstep) {
                    log(Level.ERROR, "Step changed, this probe will not collect any more");
                    return false;
                } else if (!newDefDump.equals(oldDefDump)) {
                    log(Level.TRACE, "New definition should be: %s\n", newDef);
                    upgrade(archives);
                    RrdDb.of(getPath());
                }
                log(Level.TRACE, "******");
            } else {
                create(archives);
            }
            retValue = true;
        } catch (Exception e) {
            log(Level.ERROR, e, "Store %s unusable: %s", getPath(), e);
        }
        return retValue;
    }

    /**
     * Return the date of the last update of the rrd backend
     * 
     * @return The date
     */
    public Date getLastUpdate() {
        Date lastUpdate = null;
        try (RrdDb rrdDb = factory.getRrd(getPath())){
            lastUpdate = Util.getDate(rrdDb.getLastUpdateTime());
        } catch (Exception e) {
            throw new RuntimeException("Unable to get last update date for " + p.getQualifiedName(), e);
        }
        return lastUpdate;
    }

    @Override
    public AbstractExtractor<FetchData> getExtractor() {
        try (RrdDb rrdDb = factory.getRrd(getPath())){
            return new RrdDbExtractor(rrdDb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to access rrd file  " + getPath(), e);
        }
    }

    public Map<String, Number> getLastValues() {
        Map<String, Number> retValues = new HashMap<String, Number>();
        try (RrdDb rrdDb = factory.getRrd(getPath())){
            String[] dsNames = rrdDb.getDsNames();
            for(int i = 0; i < dsNames.length; i++) {
                retValues.put(dsNames[i], rrdDb.getDatasource(i).getLastValue());
            }
        } catch (Exception e) {
            log(Level.ERROR, e, "Unable to get last values: %s", e);
        }
        return retValues;
    }

    public void commit(JrdsSample sample) {
        try (RrdDb rrdDb = factory.getRrd(getPath())){
            Sample onesample = rrdDb.createSample(sample.getTime().getTime() / 1000);
            for(Map.Entry<String, Number> e: sample.entrySet()) {
                onesample.setValue(e.getKey(), e.getValue().doubleValue());
            }
            if(p.getNamedLogger().isDebugEnabled())
                log(Level.DEBUG, "%s", onesample.dump());
            onesample.update();
        } catch (IOException e) {
            log(Level.ERROR, e, "Error while committing to rrd db: %s", e);
        }
    }

    @Override
    public RrdDb getStoreObject() {
        try {
            return factory.getRrd(getPath());
        } catch (IOException e) {
            log(Level.ERROR, e, "Failed to access rrd file %s: %s ", getPath(), e);
            return null;
        }
    }

    @Override
    public void closeStoreObject(Object rrdDb) {
        if(rrdDb != null) {
            factory.releaseRrd((RrdDb) rrdDb);
        }
    }

}
