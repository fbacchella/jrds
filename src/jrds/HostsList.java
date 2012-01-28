
package jrds;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import jrds.PropertiesManager.TimerInfo;
import jrds.configuration.ConfigObjectFactory;
import jrds.factories.ArgFactory;
import jrds.factories.ProbeMeta;
import jrds.graphe.Sum;
import jrds.starter.HostStarter;
import jrds.starter.Starter;
import jrds.starter.StarterNode;
import jrds.webapp.ACL;
import jrds.webapp.DiscoverAgent;
import jrds.webapp.RolesACL;

import org.apache.log4j.Level;

/**
 * The central repository of all informations : hosts, graph, and everything else
 * @author Fabrice Bacchella 
 */
public class HostsList extends StarterNode {

    static final private AtomicInteger generation = new AtomicInteger(0);

    private final int thisgeneration = generation.incrementAndGet();
    private final Set<HostInfo> hostList = new HashSet<HostInfo>();
    private final Map<String, jrds.starter.Timer> timers = new HashMap<String, jrds.starter.Timer>();
    private final Map<Integer, GraphNode> graphMap = new HashMap<Integer, GraphNode>();
    private final Map<Integer, Probe<?,?>> probeMap= new HashMap<Integer, Probe<?,?>>();
    private final Map<String, GraphTree> treeMap = new LinkedHashMap<String, GraphTree>(3);
    private final Map<String, Filter> filters = new TreeMap<String, Filter>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Tab> tabs = new LinkedHashMap<String, Tab>();
    private String firstTab = null;
    private Renderer renderer = null;
    private Timer collectTimer;
    private File tmpDir = null;
    // The list of roles known to jrds
    private Set<String> roles = new HashSet<String>();
    private Set<String> defaultRoles = Collections.emptySet();
    // A global flag that tells globally that this HostsList can be used
    volatile private boolean started = false;
    private Set<Class<? extends DiscoverAgent>> daList = new HashSet<Class<? extends DiscoverAgent>>();

    /**
     *  
     */
    public HostsList() {
        super();
        init();
    }

    /**
     *  
     */
    public HostsList(PropertiesManager pm) {
        super();
        init();
        configure(pm);
    }

    private void init() {
        filters.put(Filter.EVERYTHING.getName(), Filter.EVERYTHING);

        addTree(Filter.ALLHOSTS.getName(), GraphTree.HOSTROOT);
        filters.put(Filter.ALLHOSTS.getName(), Filter.ALLHOSTS);

        GraphTree viewtree = addTree(Filter.ALLVIEWS.getName(), GraphTree.VIEWROOT);
        viewtree.addPath("Services");
        filters.put(Filter.ALLVIEWS.getName(), Filter.ALLVIEWS);

        treeMap.put(Filter.ALLSERVICES.getName(), Filter.ALLSERVICES.setRoot(viewtree));
        filters.put(Filter.ALLSERVICES.getName(), Filter.ALLSERVICES);

        addTree("All tags", GraphTree.TAGSROOT);
    }

    public void configure(PropertiesManager pm) {
        started = false;
        try {
            jrds.JrdsLoggerConfiguration.configure(pm);
        } catch (IOException e1) {
            log(Level.ERROR, e1, "Unable to set log file to " + pm.logfile);
        }

        if(pm.rrddir == null) {
            log(Level.ERROR, "Probes directory not configured, can't configure");
            return;
        }

        if(pm.configdir == null) {
            log(Level.ERROR, "Configuration directory not configured, can't configure");
            return;
        }

        setTimeout(pm.timeout);
        setStep(pm.step);

        log(Level.TRACE, "timers to build %s", pm.timers);

        for(Map.Entry<String, TimerInfo> e: pm.timers.entrySet()) {
            jrds.starter.Timer t = new jrds.starter.Timer(e.getKey(), e.getValue());
            t.setParent(this);
            timers.put(e.getKey(), t);
        }
        log(Level.DEBUG, "timers %s", timers);

        renderer = new Renderer(50, tmpDir);

        log(Level.DEBUG, "Starting parsing descriptions");
        ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);
        conf.setGraphDescMap();
        Collection<ProbeDesc> probesdesc = conf.setProbeDescMap().values();

        Set<Class<? extends Starter>> externalStarters = new HashSet<Class<? extends Starter>>();
        for(ProbeDesc pd: probesdesc) {
            for(ProbeMeta meta: ArgFactory.enumerateAnnotation(pd.getProbeClass(), ProbeMeta.class, StarterNode.class)) {
                daList.add(meta.discoverAgent());
                externalStarters.add(meta.topStarter());
            }
        }
        conf.setMacroMap();

        Set<String> hostsTags = new HashSet<String>();
        conf.setHostMap(timers);

        //We try to load top level starter defined in probes
        log(Level.DEBUG, "External top starters added %s", externalStarters);
        for(jrds.starter.Timer timer: timers.values()) {
            for(HostStarter host: timer.getAllHosts()) {
                hostList.add(host.getHost());
                hostsTags.addAll(host.getTags());
                host.configureStarters(pm);
                for(Probe<?,?> p: host.getAllProbes()) {
                    p.configureStarters(pm);
                    try {
                        addProbe(p);
                    } catch (Exception e) {
                        log(Level.ERROR, e, "Error inserting probe " + p);
                    }
                }
            }
            for(Class<? extends Starter> starterClass: externalStarters) {
                try {
                    timer.registerStarter(starterClass.newInstance());
                } catch (Exception e) {
                    log(Level.ERROR, e, "Starter %s failed to register: %s", starterClass, e);
                }
            }
            timer.configureStarters(pm);            
        }

        //Configure the default ACL of all automatic filters
        for(Filter filter: filters.values()) {
            filter.addACL(pm.defaultACL);
        }

        Set<Tab> allTabs = new HashSet<Tab>();

        //Let's build the tab for all the tags
        Tab tagsTab = new Tab.Filters("All tags", PropertiesManager.TAGSTAB);
        for(String tag: hostsTags) {
            Filter f = new FilterTag(tag);
            filters.put(f.getName(), f);
            tagsTab.add(f.getName());
        }
        allTabs.add(tagsTab);

        //Let's build the tab with all the filters
        Tab filterTab = new Tab.Filters("All filters", PropertiesManager.FILTERTAB);
        Map <String, Filter> f = conf.setFilterMap();
        for(Filter filter: f.values()) {
            addFilter(filter);
            filterTab.add(filter.getName());
        }
        allTabs.add(filterTab);

        //Let's build all the custom graph tabs
        Map<String, Tab> customTabMap = conf.setTabMap();
        log(Level.DEBUG, "Tabs to add: %s", customTabMap.values());
        for(Tab t: customTabMap.values()) {
            t.setHostlist(this);
            GraphTree tabtree = t.getGraphTree();
            if(tabtree != null)
                treeMap.put(t.getName(), tabtree);
            allTabs.add(t);
        }

        log(Level.DEBUG, "Parsing graphs configuration");
        Map<String, GraphDesc> graphs = conf.setGrapMap();
        //Let's build the tab with all the custom graphs
        Tab customGraphsTab = new Tab.DynamicTree("Custom graphs", PropertiesManager.CUSTOMGRAPHTAB);
        if(! graphs.isEmpty()) {
            for(GraphDesc gd: graphs.values()) {
                AutonomousGraphNode gn = new AutonomousGraphNode(gd);
                gn.configure(this);
                graphMap.put(gn.getQualifieName().hashCode(), gn);
                customGraphsTab.add(gn.getQualifieName(), gn.getGraphDesc().getHostTree(gn));
            }
            allTabs.add(customGraphsTab);
        }

        //Let's build the tab with all the sums
        Map<String, Sum> sums = conf.setSumMap();
        if(sums.size() > 0) {
            for(Sum s: sums.values()) {
                s.configure(this);
                graphMap.put(s.getQualifieName().hashCode(), s);
                customGraphsTab.add(s.getQualifieName(), "Sums", s.getName());
            }
        }

        makeTabs(pm, conf, allTabs, customTabMap);

        //Hosts list adopts all tabs
        for(Tab t: tabs.values()) {
            t.setHostlist(this);
        }

        if(pm.security) {
            for(GraphNode gn: graphMap.values()) {
                gn.addACL(pm.defaultACL);
                checkRoles(gn, GraphTree.HOSTROOT, gn.getTreePathByHost());
                checkRoles(gn, GraphTree.VIEWROOT, gn.getTreePathByView());
            }
        }
        started = true;
    }

    public void startTimers() {
        if(started)
            collectTimer = new Timer("jrds-main-timer/" + thisgeneration, true);
        for(jrds.starter.Timer t: timers.values()) {
            t.startTimer(collectTimer);  
        }
    }

    /**
     * @param started the started to set
     */
    public void stopTimers() {
        started = false;
        if(collectTimer != null)
            collectTimer.cancel();
        collectTimer = null;
    }

    private void makeTabs(PropertiesManager pm, ConfigObjectFactory conf, Set<Tab> moretabs, Map<String, Tab> customTabMap){
        moretabs.add(new Tab("Administration", PropertiesManager.ADMINTAB) {
            @Override
            public String getJSCallback() {
                return "setAdminTab";
            }
        });
        moretabs.add(new Tab.StaticTree("All services", PropertiesManager.SERVICESTAB, getGraphTreeByView().getByPath(GraphTree.VIEWROOT, "Services")));
        moretabs.add( new Tab.StaticTree("All hosts", PropertiesManager.HOSTSTAB, getGraphTreeByHost()));
        moretabs.add(new Tab.StaticTree("All views", PropertiesManager.VIEWSTAB, getGraphTreeByView()));
        Map<String, Tab> tabsmap = new HashMap<String, Tab>(moretabs.size());
        for(Tab t: moretabs) {
            if(t != null)
                tabsmap.put(t.getId(), t);
        }
        log(Level.TRACE, "Looking for tabs list %s in %s", pm.tabsList, moretabs);
        for(String tabid: pm.tabsList) {
            if("@".equals(tabid)) {
                for(Tab t: customTabMap.values()) {
                    tabs.put(t.getId(), t);
                }
            }
            else {
                Tab t = tabsmap.get(tabid);
                if(t == null) {
                    log(Level.ERROR, "Non existent tab to add: " + tabid);
                    continue;
                }
                tabs.put(tabid, t);
            }
        }
        //Search for the first valid tab id
        for(int i=0; i < pm.tabsList.size(); i++ ) {
            firstTab = pm.tabsList.get(i);
            if(tabs.containsKey(firstTab))
                break;
        }
    }

    public Collection<HostInfo> getHosts() {
        return hostList;
    }

    public Set<String> getTabsId() {
        return tabs.keySet();
    }

    public Tab getTab(String id) {
        return tabs.get(id);
    }

    /**
     * Create a new graph tree
     * @param label The name of the tree
     * @param root The name of the first element in the tree
     * @return a graph tree
     */
    private GraphTree addTree(String label, String root) {
        if( ! treeMap.containsKey(label)) {
            GraphTree newTree = GraphTree.makeGraph(root);
            treeMap.put(label, newTree);
            return newTree;
        }
        return null;
    }

    public void addGraphs(Collection<GraphNode> graphs) {
        for(GraphNode currGraph: graphs) {
            LinkedList<String> path;

            path = currGraph.getTreePathByHost();
            getGraphTreeByHost().addGraphByPath(path, currGraph);

            path = currGraph.getTreePathByView();
            getGraphTreeByView().addGraphByPath(path, currGraph);

            graphMap.put(currGraph.hashCode(), currGraph);
        }
    }

    /**
     * Generate the list of roles that might view this node, using the filters
     * @param gn
     * @param pathList
     */
    private void checkRoles(GraphNode gn, String root, List<String> pathList) {
        StringBuilder path = new StringBuilder("/" + root);
        for(String pathElem: pathList) {
            path.append("/").append(pathElem);
        }
        for(Filter f: filters.values()) {
            if(f.acceptGraph(gn, path.toString())) {
                log(Level.TRACE, "Adding ACL %s to %s", f.getACL(), gn);
                gn.addACL(f.getACL());
            }
        }
    }

    public void addHost(HostInfo newhost) {
        hostList.add(newhost);
    }


    public GraphTree getGraphTree(String name) {
        return treeMap.get(name);
    }

    public Set<String> getTreesName() {
        return treeMap.keySet();
    }

    public Collection<GraphTree> getTrees() {
        return treeMap.values();
    }

    public GraphTree getGraphTreeByHost() {
        return treeMap.get(Filter.ALLHOSTS.getName());
    }

    public GraphTree getGraphTreeByView() {
        return treeMap.get(Filter.ALLVIEWS.getName());
    }

    /**
     * Return a graph identified by his hash value
     * @param id the hash value of the graph
     * @return the graph found or null of nothing found
     */
    public GraphNode getGraphById(int id) {
        return graphMap.get(id);
    }

    /**
     * Return a probe identified by his hash value
     * @param id the hash value of the probe
     * @return the probe found or null of nothing found
     */
    public Probe<?,?> getProbeById(int id) {
        return probeMap.get(id);
    }

    /**
     * Return a probe identified by path
     * @param host the host
     * @param probeName the probe name: the probeName element of a probedesc
     * @return the graph found or null of nothing found
     */
    public Probe<?,?> getProbeByPath(String host, String probeName) {
        String path = host + "/" + probeName;
        return probeMap.get(path.hashCode());
    }

    public void addProbe(Probe<?,?> p) {
        probeMap.put(p.hashCode(), p);
        addGraphs(p.getGraphList());
    }

    public GraphTree getNodeById(int id) {
        GraphTree node = null;
        for(GraphTree tree: treeMap.values()) {
            node = tree.getById(id);
            if(node != null)
                return node;
        }
        return node;
    }

    private void addFilter(Filter newFilter) {
        filters.put(newFilter.getName(), newFilter);
        ACL acl = newFilter.getACL();
        if(acl instanceof RolesACL) {
            roles.addAll(((RolesACL) acl).getRoles());
        }
        log(Level.DEBUG, "Filter %s added with ACL %s", newFilter.getName(), newFilter.getACL());
    }

    public Filter getFilter(String name) {
        Filter retValue = null;
        if(name != null)
            retValue = filters.get(name);
        return retValue;
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    /**
     * @return the renderer
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /* (non-Javadoc)
     * @see jrds.starter.StarterNode#isCollectRunning()
     */
    @Override
    public boolean isCollectRunning() {
        return started;
    }

    /**
     * @return the roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * @return the defaultRoles
     */
    public Set<String> getDefaultRoles() {
        return defaultRoles;
    }

    public Set<DiscoverAgent> getDiscoverAgent() {
        Set<DiscoverAgent> daSet = new HashSet<DiscoverAgent>(daList.size());
        for(Class<? extends DiscoverAgent> daa: daList) {
            try {
                daSet.add(daa.newInstance());
            } catch (Exception e) {
                log(Level.ERROR, e, "Error creating discover agent " + daa.getName() + ": " + e);
            }
        }

        return daSet;
    }

    /**
     * @return the first tab
     */
    public String getFirstTab() {
        return firstTab;
    }

    /**
     * @return the timers
     */
    public Iterable<jrds.starter.Timer> getTimers() {
        return timers.values();
    }

    /**
     * @return the thisgeneration
     */
    public int getGeneration() {
        return thisgeneration;
    }
}