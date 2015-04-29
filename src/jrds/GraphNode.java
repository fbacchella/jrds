package jrds;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import jrds.configuration.HostBuilder;
import jrds.factories.ArgFactory;
import jrds.store.ExtractInfo;
import jrds.webapp.ACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.rrd4j.data.DataProcessor;

/**
 * @author Fabrice Bacchella
 *
 */
public class GraphNode implements Comparable<GraphNode>, WithACL {

    static final private Logger logger = Logger.getLogger(GraphNode.class);

    protected Probe<?,?> probe;
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
    public GraphNode(Probe<?,?> theStore, GraphDesc gd) {
        this.probe = theStore;
        this.gd = gd;
        this.acl = gd.getACL();
    }

    /**
     * A protected constructor
     * child are allowed to build themselves in a strange way
     * 
     */
    protected GraphNode() {
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getQualifiedName().hashCode();
    }

    /**
     * @return Returns the theStore.
     */
    public Probe<?,?> getProbe() {
        return probe;
    }

    /**
     * To be called if the probe was not provided in the initial creation
     * This should be called as soon as possible
     * @param probe a custom generated probe
     */
    protected void setProbe(Probe<?,?> probe) {
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
     * @return
     */
    public String getQualifiedName() {
        if (probe.getHost() != null) {
            return probe.getHost().getName() + "/"  + getName();
        } else {
            return "/"  + getName();
        }
    }

    public GraphDesc getGraphDesc() {
        return gd;
    }

    /**
     * To be called if the graphdesc was not provided in the initial creation
     * This should be called as soon as possible
     * @param gd A custom generated GraphDesc
     */
    protected void setGraphDesc(GraphDesc gd) {
        this.gd = gd;
        this.acl = gd.getACL();
    }

    public Graph getGraph() {
        Class<Graph>  gclass = gd.getGraphClass();

        try {
            Graph g =  gclass.getConstructor(GraphNode.class).newInstance(this);
            Map<String, GenericBean> beansList = ArgFactory.getBeanPropertiesMap(gclass, Graph.class);

            //Resolve the beans
            for(Map.Entry<String, String> e: beans.entrySet()) {
                String name = Util.parseTemplate(e.getKey(), probe);
                String textValue = Util.parseTemplate(e.getValue(), probe);
                GenericBean bean = beansList.get(name);
                if(bean == null) {
                    logger.error(String.format("Unknown bean for %s: %s", gd.getName() , name));
                    continue;
                }
                logger.trace(Util.delayedFormatString("Found attribute %s with value %s", name, textValue));
                bean.set(g, textValue);
            }
            return g;
        } catch (Exception e) {
            throw new RuntimeException(HostBuilder.class.getName(), e);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(GraphNode arg0) {
        if (viewPath == null)
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

}
