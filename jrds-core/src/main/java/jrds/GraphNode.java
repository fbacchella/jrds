package jrds;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import org.rrd4j.data.DataProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.factories.ArgFactory;
import jrds.store.ExtractInfo;
import jrds.webapp.ACL;
import jrds.webapp.WithACL;

/**
 * @author Fabrice Bacchella
 *
 */
public class GraphNode implements Comparable<GraphNode>, WithACL {

    static final private Logger logger = LoggerFactory.getLogger(GraphNode.class);

    protected Probe<?, ?> probe;
    private String viewPath = null;
    private GraphDesc gd;
    private String name = null;
    private String graphTitle = null;
    private ACL acl = ACL.ALLOWEDACL;
    private PlottableMap customData = PlottableMap.Empty;
    private Map<String, String> beans = Collections.emptyMap();

    /**
     *
     */
    public GraphNode(Probe<?, ?> theStore, GraphDesc gd) {
        this.probe = theStore;
        this.gd = gd;
        this.acl = gd.getACL();
    }

    /**
     * A protected constructor child are allowed to build themselves in a
     * strange way
     * 
     */
    protected GraphNode() {
    }

    /**
     * @return Returns the theStore.
     */
    public Probe<?, ?> getProbe() {
        return probe;
    }

    /**
     * To be called if the probe was not provided in the initial creation This
     * should be called as soon as possible
     * 
     * @param probe a custom generated probe
     */
    protected void setProbe(Probe<?, ?> probe) {
        this.probe = probe;
    }

    public LinkedList<String> getTreePathByHost() {
        return gd.getHostTree(this);
    }

    public LinkedList<String> getTreePathByView() {
        return gd.getViewTree(this);
    }

    private String parseTemplate(String template) {
        Object[] arguments = {
                "${graphdesc.name}",
                "${host}",
                "${index}",
                "${url}",
                "${probename}",
                "${index.signature}",
                "${url.signature}"
        };
        return jrds.Util.parseOldTemplate(template, arguments, probe, gd);
    }

    public String getGraphTitle() {
        if(graphTitle == null) {
            graphTitle = parseTemplate(gd.getGraphTitle());
        }
        return graphTitle;
    }

    public String getName() {
        if(name == null) {
            name = parseTemplate(gd.getGraphName());
        }
        return name;
    }

    /**
     * Return a uniq name for the graph
     * 
     * @return
     */
    public String getQualifiedName() {
        if(probe.getHost() != null) {
            return probe.getHost().getName() + "/" + getName();
        } else {
            return "/" + getName();
        }
    }

    public GraphDesc getGraphDesc() {
        return gd;
    }

    /**
     * To be called if the graphdesc was not provided in the initial creation
     * This should be called as soon as possible
     * 
     * @param gd A custom generated GraphDesc
     */
    protected void setGraphDesc(GraphDesc gd) {
        this.gd = gd;
        this.acl = gd.getACL();
    }

    public Graph getGraph() {
        Class<Graph> gclass = gd.getGraphClass();

        Graph g;
        Map<String, GenericBean> beansList;
        try {
            g = gclass.getConstructor(GraphNode.class).newInstance(this);
            beansList = ArgFactory.getBeanPropertiesMap(gclass, Graph.class);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            Util.log(this, logger, Level.ERROR, e, "Failed to build a graph instance %s: %s", gclass.getCanonicalName(), e);
            return null;
        }

        // Resolve the beans
        for(Map.Entry<String, String> e: beans.entrySet()) {
            String name = Util.parseTemplate(e.getKey(), probe);
            String textValue = Util.parseTemplate(e.getValue(), probe);
            GenericBean bean = beansList.get(name);
            if(bean == null) {
                logger.error("Unknown bean for {}: {}", gd.getName(), name);
                continue;
            }
            logger.trace("Found attribute {} with value {}", name, textValue);
            bean.set(g, textValue);
        }
        return g;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(GraphNode arg0) {
        if(viewPath == null)
            viewPath = this.getTreePathByView().toString();

        String otherPath = arg0.getTreePathByView().toString();

        return String.CASE_INSENSITIVE_ORDER.compare(viewPath, otherPath);
    }

    @Override
    public String toString() {
        return probe.toString() + "/" + getName();
    }

    public void addACL(ACL acl) {
        this.acl = this.acl.join(acl);
    }

    public ACL getACL() {
        return acl;
    }

    /**
     * @return the customData
     */
    public PlottableMap getCustomData() {
        return customData;
    }

    /**
     * @param customData the customData to set
     */
    public void setCustomData(PlottableMap customData) {
        this.customData = customData;
    }

    public DataProcessor getPlottedDate(ExtractInfo ei) throws IOException {
        PlottableMap pm = getCustomData();
        pm.configure(ei);
        DataProcessor dp = gd.getPlottedDatas(probe, ei, pm);
        dp.processData();
        return dp;
    }

    public Map<String, String> getBeans() {
        return beans;
    }

    public void setBeans(Map<String, String> beans) {
        this.beans = beans;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getQualifiedName().hashCode();
    }

    /**
     * Two nodes are equals if qualified names matches
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            return getQualifiedName().equals(obj);
        }
    }

}
