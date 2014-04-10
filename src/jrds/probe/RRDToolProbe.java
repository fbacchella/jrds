package jrds.probe;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.rrd4j.core.jrrd.RRDatabase;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Probe;
import jrds.factories.ProbeBean;
import jrds.graphe.RRDToolGraphNode;
import jrds.store.RRDToolStoreFactory;
import jrds.store.StoreFactory;

/**
 * A class wrapper to use rrdtool's files
 * @author Fabrice Bacchella
 *
 */
@ProbeBean({"rrdfile"})
public class RRDToolProbe extends Probe<String, Double> {
    private File rrdpath;

    public Boolean configure(File rrdpath) {
        this.rrdpath = rrdpath;
        try {
            log(Level.TRACE, "rrd is %s", rrdpath.getCanonicalPath());
        } catch (IOException e) {
        }
        Map<String, String> args = new HashMap<String, String>(1);
        try {
            args.put("rrdfile", rrdpath.getCanonicalPath());
            setMainStore(new RRDToolStoreFactory(), args);
            return rrdpath.canRead();
        } catch (IOException e) {
            log(Level.ERROR, e, "rrdtool file %s unreadable", rrdpath);
            return false;
        } catch (InvocationTargetException e) {
            log(Level.ERROR, e, "store configuration failed");
            return false;
        }
    }

    /* (non-Javadoc)
     * @see jrds.Probe#setMainStore(jrds.store.StoreFactory, java.util.Map)
     */
    @Override
    public void setMainStore(StoreFactory factory, Map<String, String> args)
            throws InvocationTargetException {
        if(factory.getClass() == RRDToolStoreFactory.class)
            super.setMainStore(factory, args);
    }

    public void setRrdfile(File rrdpath) {
        this.rrdpath = rrdpath;
    }

    public File getRrdfile() {
        return rrdpath;
    }

    /* (non-Javadoc)
     * @see jrds.Probe#addGraph(jrds.GraphNode)
     */
    @Override
    public void addGraph(GraphNode node) {
        super.addGraph(new RRDToolGraphNode(this, node.getGraphDesc(), rrdpath));
    }

    /* (non-Javadoc)
     * @see jrds.Probe#addGraph(jrds.GraphDesc)
     */
    @Override
    public void addGraph(GraphDesc gd) {
        super.addGraph(new RRDToolGraphNode(this, gd, rrdpath));
    }

    @Override
    public Map<String, Double> getNewSampleValues() {
        return Collections.emptyMap();
    }

    @Override
    public String getSourceType() {
        return "RRDToolFile";
    }

    /* (non-Javadoc)
     * @see jrds.Probe#collect()
     */
    @Override
    public void collect() {
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
            log(Level.ERROR, "probe %s, read failed: %s", e.getMessage());
        }
        return new Date(0);
    }

}
