/*
 * Created on 22 nov. 2004
 */
package jrds;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jrobin.core.ArcDef;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;
import org.jrobin.core.Util;

import jrds.GraphFactory;

/**
 * A abstract class that needs to be derived for specific probe.<br>
 * the derived class must construct a <code>ProbeDesc</code> and
 * can overid some method as needed
 * @author Fabrice Bacchella
 */
public abstract class Probe
    implements Comparable {

    static final private Logger logger = JrdsLogger.getLogger(Probe.class);
    static final private RrdDbPool pool = RrdDbPool.getInstance();

    private RrdDb rrdDb;
    private String name;
    private RdsHost monitoredHost;
    private Collection graphList;
    private boolean isOpen = false;
    private Integer lock = new Integer(1);
    private String stringValue = null;
    private ProbeDesc pd;

    /**
     * The constructor that should be called by derived class
     * @param monitoredHost
     * @param pd
     */
    public Probe(RdsHost monitoredHost, ProbeDesc pd) {
        name = null;
        rrdDb = null;
        this.monitoredHost = monitoredHost;
        this.pd = pd;
    }

    public RdsHost getHost() {
        return monitoredHost;
    }

    private Collection initGraphList() {
        Collection graphClasses = pd.getGraphClasses();
        Collection graphList = new ArrayList(graphClasses.size());
        Class[] thisClass = new Class[] {
            Probe.class};
        Object[] args = new Object[] {
            this};
        for (Iterator i = graphClasses.iterator(); i.hasNext(); ) {
            Object o = i.next();
            RdsGraph newGraph = GraphFactory.makeGraph(o, this);
            if(newGraph != null)
                graphList.add(newGraph);
        }
        return graphList;
    }

    /**
     * @return Returns the graphList.
     */
    public Collection getGraphList() {
        if (graphList == null)
            graphList = initGraphList();
        return graphList;
    }

    public String getName() {
        if (name == null)
            name = getPd().getRrdName();
        return name;
    }

    public String getRrdName() {
        return monitoredHost.getHostDir() +
            PropertiesManager.getInstance().fileSeparator + getName() + ".rrd";
    }

    public void setRrdName(String rrdName) {
        if (!pd.isCloned())
            pd = (ProbeDesc) pd.clone();
        pd.setRrdName(rrdName);
    }

    protected DsDef[] getDsDefs() throws RrdException {
        return getPd().getDsDefs();
    }

    protected RrdDef getDefaultRrdDef() throws RrdException {
        RrdDef def = new RrdDef(getRrdName());
        return def;

    }

    private ArcDef[] getArcDefs() throws RrdException {
        ArcDef[] defaultArc = new ArcDef[3];
        defaultArc[0] = new ArcDef("AVERAGE", 0.5, 1, 105120);
        defaultArc[1] = new ArcDef("AVERAGE", 0.5, 12, 8760);
        defaultArc[2] = new ArcDef("AVERAGE", 0.5, 288, 730);
        return defaultArc;
    }

    /**
     * @throws RrdException
     * @throws IOException
     */
    private void create() throws RrdException, IOException {
        synchronized (lock) {
            logger.info("Need to create rrd " + this);
            RrdDef def = getDefaultRrdDef();
            def.addArchive(getArcDefs());
            def.addDatasource(getDsDefs());
            rrdDb = pool.requestRrdDb(def);
            rrdDb.sync();
            isOpen = true;
        }
    }

    /**
     * Open the rrd backend of the probe.
     * it's created if it's needed
     * @throws IOException
     * @throws RrdException
     */
    public void open() throws IOException, RrdException {
        synchronized (lock) {
            if (rrdDb == null) {
                File rrdDir = new File(monitoredHost.getHostDir());
                if (!rrdDir.isDirectory()) {
                    rrdDir.mkdir();
                }
                File rrdFile = new File(getRrdName());
                if (rrdFile.isFile()) {
                    rrdDb = pool.requestRrdDb(getRrdName());
                    isOpen = true;
                }
                else
                    create();
            }
        }
    }

    /**
     * The method that return a map of data to be stored.<br>
     * the key is resolved using the <code>ProbeDesc</code>. A key not associated with an existent datastore will generate a warning
     * but will not prevent the other values to be stored.<br>
     * the value must be a <code>java.lang.Number<code><br>
     * @return the map of values
     */
    public abstract Map getNewSampleValues();

    /**
     * A method that might be overiden if specif treatement is needed
     * by a prope.<br>
     * By default, it does nothing.
     * @param valuesList
     * @return an map of value to be stored
     */
    public Map filterValues(Map valuesList) {
        return valuesList;
    }

    /**
     * Store the values on the rrd backend.
     * Overiding should be avoided.
     * @param oneSample
     */
    protected void updateSample(Sample oneSample) {
        Map sampleVals = getNewSampleValues();
        Map nameMap = getPd().getDsNameMap();
        if (sampleVals != null) {
            sampleVals = filterValues(sampleVals);
            for (Iterator i = sampleVals.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                String dsName = (String) nameMap.get(e.getKey());
                double value = ( (Number) e.getValue()).doubleValue();
                if (dsName != null)
                    try {
                        oneSample.setValue(dsName, value);
                    }
                    catch (RrdException e1) {
                        logger.warn("Unable to update value " + value +
                                    " from " + this +": " +
                                    e1.getLocalizedMessage());
                    }
            }
        }

    }

    /**
     * Launch an collect of values.
     * It cannot be overiden
     * @throws IOException
     * @throws RrdException
     */
    public final void collect() throws IOException, RrdException {
        logger.debug("launch collect for " + this);
        if (isOpen) {
            Sample onesample;
            try {
                onesample = rrdDb.createSample();
                updateSample(onesample);
                logger.debug(onesample.dump());
                onesample.update();
            }
            catch (ArithmeticException ex) {
                logger.warn("Error while storing sample:" +
                            ex.getLocalizedMessage());
            }
        }
        else {
            logger.warn("trying to update store " + this +" wich is closed.");
        }
    }

    /**
     * Commit the result of a collect
     * @throws IOException
     */
    public final void sync() throws IOException {
        if (isOpen) {
            rrdDb.sync();
            File rrdfile = new File(rrdDb.getCanonicalPath());
            rrdfile.setLastModified(System.currentTimeMillis());
        }
    }

    public final void close() throws IOException, RrdException {
        if (isOpen) {
            pool.release(rrdDb);
        }
        if (rrdDb.isClosed())
            isOpen = false;
    }

    /**
     * Return the string value of the probe as a path constitued of
     * the host name / the probe name
     * @see java.lang.Object#toString()
     */
    public final String toString() {
        if (stringValue == null) {
            stringValue = getHost().getName() + "/" + getName();
        }
        return stringValue;
    }

    /**
     * The comparaison order of two object of ths class is a case insensitive
     * comparaison of it's string value.
     *
     * @param arg0 Object
     * @return int
     */
    public final int compareTo(Object arg0) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.toString(),
            arg0.toString());
    }

    /**
     * @return Returns the <code>ProbeDesc</code> of the probe.
     */
    public ProbeDesc getPd() {
        return pd;
    }

    /**
     * @return Returns the rrdDb.
     */
    protected RrdDb getRrdDb() {
        return rrdDb;
    }

    /**
     * Return the date of the last update of the rrd backend
     * @return The date
     * @throws IOException
     */
    public Date getLastUpdate() throws IOException {
        return Util.getDate(rrdDb.getLastUpdateTime());
    }
}
