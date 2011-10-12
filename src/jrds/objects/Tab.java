package jrds.objects;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.Util;

import org.apache.log4j.Logger;

public abstract class Tab {
    public static final class Filters extends Tab {
        private final Set<String> filters = new TreeSet<String>(Util.nodeComparator);

        public Filters(String name) {
            super(name);
        }
        public Filters(String name, String id) {
            super(name, id);
        }
        public void add(String filter) {
            filters.add(filter);
        }
        public Set<jrds.objects.Filter> getFilters() {
            Set<jrds.objects.Filter> filtersset = new LinkedHashSet<jrds.objects.Filter>(filters.size());
            for(String filtername: filters) {
                jrds.objects.Filter f = hostlist.getFilter(filtername);
                if(f != null)
                    filtersset.add(f);
            }
            return filtersset;
        }
        public boolean isFilters() {
            return true;
        }
    }
    public static final class StaticTree extends Tab {
        private final GraphTree gt;
        public StaticTree(String name, GraphTree gt) {
            super(name);
            this.gt = gt;
        }
        public StaticTree(String name, String id, GraphTree gt) {
            super(name, id);
            this.gt = gt;
        }
        public GraphTree getGraphTree() {
            return gt;
        }
    }
    public static final class DynamicTree extends Tab {
        private final Map<String, List<String>> paths = new TreeMap<String, List<String>>(Util.nodeComparator);
        public DynamicTree(String name) {
            super(name);
        }
        public DynamicTree(String name, String id) {
            super(name, id);
        }
        public void add(String id, List<String> path) {
            paths.put(id, path);
        }
        public GraphTree getGraphTree() {
            GraphTree gt = GraphTree.makeGraph(name); 
            for(Map.Entry<String , List<String>> e: paths.entrySet()) {
                String id = e.getKey();
                List<String> path = e.getValue();
                GraphNode gn = hostlist.getGraphById(id.hashCode());
                if(gn == null) {
                    logger.warn(jrds.Util.delayedFormatString("Graph not found for %s: %s", name, id));
                    continue;
                }
                gt.addGraphByPath(path, gn);
            }
            return gt;
        }
    }

    static protected final Logger logger = Logger.getLogger(Tab.class);

    protected String name;
    protected String id;
    protected HostsList hostlist;

    protected Tab(String name) {
        this.name = name;
        this.id = name;
    }

    protected Tab(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public void add(String filter) {
        throw new RuntimeException("Not implemented");
    }

    public void add(String id, String... path) {
        add(id, Arrays.asList(path));
    }
    
    public void add(String id, List<String> path) {
        throw new RuntimeException("Not implemented");
    }

    public GraphTree getGraphTree() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @param hostlist the hostlist to set
     */
    public void setHostlist(HostsList hostlist) {
        this.hostlist = hostlist;
    }

    public Set<jrds.objects.Filter> getFilters() {
        throw new RuntimeException("Not implemented");
    }

    /* (non-Javadoc)
     * @see jrds.Probe#toString()
     */
    @Override
    public String toString() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public boolean isFilters() {
        return false;
    }

    public String getJSCallback() {
        return "treeTabCallBack";
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
}
