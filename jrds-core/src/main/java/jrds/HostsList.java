
package jrds;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.PropertiesManager.TimerInfo;
import jrds.configuration.ConfigObjectFactory;
import jrds.configuration.ProbeClassResolver;
import jrds.factories.ArgFactory;
import jrds.factories.ProbeMeta;
import jrds.graphe.Sum;
import jrds.starter.HostStarter;
import jrds.starter.Listener;
import jrds.starter.Starter;
import jrds.starter.StarterNode;
import jrds.store.StoreFactory;
import jrds.webapp.ACL;
import jrds.webapp.DiscoverAgent;
import jrds.webapp.RolesACL;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * The central repository of all informations : hosts, graph, and everything
 * else
 * 
 * @author Fabrice Bacchella
 */
public class HostsList extends StarterNode {

    static final private Logger logger = LoggerFactory.getLogger(HostsList.class);

    static final private AtomicInteger generation = new AtomicInteger(0);

    private final int thisgeneration = generation.incrementAndGet();
    private final Set<HostInfo> hostList = new HashSet<HostInfo>();
    private final Map<String, jrds.starter.Timer> timers = new HashMap<String, jrds.starter.Timer>();
    private final Map<Integer, GraphNode> graphMap = new HashMap<Integer, GraphNode>();
    private final Map<Integer, Probe<?, ?>> probeMap = new HashMap<Integer, Probe<?, ?>>();
    private final Map<String, GraphTree> treeMap = new LinkedHashMap<String, GraphTree>(3);
    private final Map<String, Filter> filters = new TreeMap<String, Filter>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Tab> tabs = new LinkedHashMap<String, Tab>();
    private String firstTab = null;
    private Renderer renderer = null;
    private Timer collectTimer;
    // The list of roles known to jrds
    private Set<String> roles = new HashSet<String>();
    private Set<String> defaultRoles = Collections.emptySet();
    private Set<Class<? extends DiscoverAgent>> daList = new HashSet<Class<? extends DiscoverAgent>>();
    private Set<StoreFactory> stores = Collections.emptySet();
    @Getter @Setter @Accessors(chain=true)
    private Function<ClassLoader, ProbeClassResolver> probeClassResolverSource = ProbeClassResolver::new;
    private ProbeClassResolver probeClassResolver;

    /**
     *  
     */
    public HostsList() {
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
        probeClassResolver = probeClassResolverSource.apply(pm.extensionClassLoader);

        if(pm.rrddir == null) {
            log(Level.ERROR, "Probes directory not configured, can't configure");
            return;
        }

        if(pm.configdir == null) {
            log(Level.ERROR, "Configuration directory not configured, can't configure");
            return;
        }

        pm.configureStores();
        stores = new HashSet<>(pm.stores.size() + 1);
        stores.addAll(pm.stores.values());
        stores.add(pm.defaultStore);
        stores.forEach(StoreFactory::start);
        stores = Collections.unmodifiableSet(stores);

        setTimeout(pm.timeout);
        setStep(pm.step);

        log(Level.TRACE, "timers to build %s", pm.timers);

        for(Map.Entry<String, TimerInfo> e: pm.timers.entrySet()) {
            jrds.starter.Timer t = new jrds.starter.Timer(e.getKey(), e.getValue());
            t.setParent(this);
            timers.put(e.getKey(), t);
        }
        log(Level.DEBUG, "timers %s", timers);

        renderer = new Renderer(50, pm.tmpdir);

        log(Level.DEBUG, "Starting parsing descriptions");
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);
        conf.setArchiveSetMap();
        conf.setGraphDescMap();
        conf.setProbeClassResolver(probeClassResolver);
        Map<String, ProbeDesc<?>> allProbeDesc = conf.setProbeDescMap();
        for(ProbeDesc<?> pd: allProbeDesc.values()) {
            for(ProbeMeta meta: ArgFactory.enumerateAnnotation(pd.getProbeClass(), ProbeMeta.class, StarterNode.class)) {
                log(Level.TRACE, "new probe meta: %s", meta);
                daList.add(meta.discoverAgent());
            }
        }
        log(Level.DEBUG, "Discover agents classes are %s", daList);
        conf.setMacroMap();
        for(Listener<?, ?> l: conf.setListenerMap().values()) {
            registerStarter(l);
        }

        Set<String> hostsTags = new HashSet<String>();
        Map<String, HostInfo> allHosts = conf.setHostMap(timers);
        hostList.addAll(allHosts.values());
        Set<Class<? extends Starter>> topStarterClasses = new HashSet<Class<? extends Starter>>();

        // We try to load top level starter defined in probes
        for(jrds.starter.Timer timer: timers.values()) {
            Set<Class<? extends Starter>> timerStarterClasses = new HashSet<Class<? extends Starter>>();
            for(HostStarter host: timer.getAllHosts()) {
                hostsTags.addAll(host.getTags());
                host.configureStarters(pm);
                for(Probe<?, ?> p: host.getAllProbes()) {
                    p.configureStarters(pm);
                    try {
                        for(ProbeMeta meta: ArgFactory.enumerateAnnotation(p.getClass(), ProbeMeta.class, StarterNode.class)) {
                            log(Level.TRACE, "another probe meta: %s", meta);
                            timerStarterClasses.add(meta.timerStarter());
                            topStarterClasses.add(meta.topStarter());
                        }
                    } catch (Exception e) {
                        log(Level.ERROR, e, "Error inserting probe " + p + ": " + e);
                    }
                }
            }
            log(Level.DEBUG, "timer starters added %s for timer %s", timerStarterClasses, timer.getName());
            for(Class<? extends Starter> starterClass: timerStarterClasses) {
                try {
                    timer.registerStarter(starterClass.getConstructor().newInstance());
                } catch (Throwable e) {
                    log(Level.ERROR, e, "Starter %s failed to register for timer %s: %s", starterClass, timer.getName(), e);
                }
            }
            timer.configureStarters(pm);
        }

        log(Level.DEBUG, "top starters added %s", topStarterClasses);
        for(Class<? extends Starter> starterClass: topStarterClasses) {
            try {
                Starter top = starterClass.getConstructor().newInstance();
                registerStarter(top);
                top.configure(pm);
            } catch (Throwable e) {
                log(Level.ERROR, e, "Top level starter %s failed to register: %s", starterClass, e);
            }
        }

        for(HostInfo host: hostList) {
            for(Probe<?, ?> p: host.getProbes()) {
                addProbe(p);
                // Some probe are done outside of a starter
                // Don't forget them
                if(p.getHostList() == null) {
                    p.setParent(this);
                }
            }
        }

        // Configure the default ACL of all automatic filters
        for(Filter filter: filters.values()) {
            filter.addACL(pm.defaultACL);
        }

        Set<Tab> allTabs = new HashSet<Tab>();

        // Let's build the tab for all the tags
        doTagsTabs(hostsTags, allTabs);

        // Let's build the tab with all the filters
        doFilterTabs(conf.setFilterMap(), allTabs);

        // Build all the sums and add them to all the graphs
        doSums(conf.setSumMap(), graphMap, treeMap, allTabs);

        // Let's build the tab with all the custom graph and add them to all
        // graph
        doCustomGraphs(conf.setGrapMap(), graphMap, treeMap, allTabs);

        // Resolve the custom tabs and generate the associated tree
        Map<String, Tab> customTabMap = conf.setTabMap();
        doCustomTabs(customTabMap, treeMap, allTabs);

        // Add the always here tabs
        allTabs.add(new Tab.StaticTree("All services", PropertiesManager.SERVICESTAB, getGraphTreeByView().getByPath(GraphTree.VIEWROOT, "Services")));
        allTabs.add(new Tab.StaticTree("All hosts", PropertiesManager.HOSTSTAB, getGraphTreeByHost()));
        allTabs.add(new Tab.StaticTree("All views", PropertiesManager.VIEWSTAB, getGraphTreeByView()));
        allTabs.add(new Tab("Administration", PropertiesManager.ADMINTAB) {
            @Override
            public String getJSCallback() {
                return "adminTabCallback";
            }
        });

        firstTab = makeTabs(pm.tabsList, allTabs, customTabMap, tabs);

        // Hosts list adopts all tabs
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
    }

    public void startTimers() {
        collectTimer = new Timer("jrds-main-timer/" + thisgeneration, true);
        for(jrds.starter.Timer t: timers.values()) {
            t.startTimer(collectTimer);
        }
        startCollect();
    }

    String makeTabs(List<String> tabsList, Set<Tab> moretabs, Map<String, Tab> customTabMap, Map<String, Tab> tabs) {
        Map<String, Tab> tabsmap = new HashMap<String, Tab>(moretabs.size());
        for(Tab t: moretabs) {
            if(t != null)
                tabsmap.put(t.getId(), t);
        }

        log(Level.TRACE, "Looking for tabs list %s in %s", tabsList, moretabs);
        String firstTab = null;
        for(String tabid: tabsList) {
            // @ is a magic place holder, used to replace if with the custom
            // tabs list
            if("@".equals(tabid)) {
                for(Tab t: customTabMap.values()) {
                    tabs.put(t.getId(), t);
                }
            } else {
                Tab t = tabsmap.get(tabid);
                if(t != null) {
                    tabs.put(tabid, t);

                    // store the first tab
                    if(firstTab == null)
                        firstTab = tabid;
                } else {
                    // Not a problem, some automatic tab might be empty (sum,
                    // customgraph, tags)
                    log(Level.DEBUG, "Non existent tab to add: " + tabid);
                }
            }
        }
        return firstTab;
    }

    void doTagsTabs(Set<String> hostsTags, Set<Tab> tabs) {
        Tab tagsTab = new Tab.Filters("All tags", PropertiesManager.TAGSTAB);
        for(String tag: hostsTags) {
            Filter f = new FilterTag(tag);
            filters.put(f.getName(), f);
            tagsTab.add(f.getName());
        }
        tabs.add(tagsTab);
    }

    void doFilterTabs(Map<String, Filter> f, Set<Tab> tabs) {
        Tab filterTab = new Tab.Filters("All filters", PropertiesManager.FILTERTAB);
        for(Filter filter: f.values()) {
            addFilter(filter);
            filterTab.add(filter.getName());
        }
        tabs.add(filterTab);
    }

    void doSums(Map<String, Sum> sums, Map<Integer, GraphNode> graphMap, Map<String, GraphTree> treeMap, Set<Tab> tabs) {
        // Let's build the tab with all the sums
        if(sums.size() > 0) {
            Tab sumGraphsTab = new Tab.DynamicTree("Sums", PropertiesManager.SUMSTAB);
            sumGraphsTab.setHostlist(this);
            for(Sum s: sums.values()) {
                try {
                    s.configure(this);
                    graphMap.put(s.getQualifiedName().hashCode(), s);
                    sumGraphsTab.add(s.getQualifiedName(), "Sums", s.getName());
                } catch (Exception e1) {
                    log(Level.ERROR, e1, "failed sum: %s", e1);
                }
            }
            GraphTree tree = sumGraphsTab.getGraphTree();
            treeMap.put(tree.getName(), tree);
            tabs.add(sumGraphsTab);
        }
    }

    void doCustomGraphs(Map<String, GraphDesc> graphs, Map<Integer, GraphNode> graphMap, Map<String, GraphTree> treeMap, Set<Tab> tabs) {
        log(Level.DEBUG, "Parsing graphs configuration");
        // Let's build the tab with all the custom graphs
        if(!graphs.isEmpty()) {
            Tab customGraphsTab = new Tab.DynamicTree("Custom graphs", PropertiesManager.CUSTOMGRAPHTAB);
            customGraphsTab.setHostlist(this);
            for(GraphDesc gd: graphs.values()) {
                AutonomousGraphNode gn = new AutonomousGraphNode(gd);
                gn.configure(this);
                graphMap.put(gn.getQualifiedName().hashCode(), gn);
                customGraphsTab.add(gn.getQualifiedName(), Arrays.asList(new String[] { gd.getName() }));
            }
            GraphTree tree = customGraphsTab.getGraphTree();
            treeMap.put(tree.getName(), tree);
            tabs.add(customGraphsTab);
        }
    }

    void doCustomTabs(Map<String, Tab> customTabMap, Map<String, GraphTree> treeMap, Set<Tab> tabs) {
        if(!customTabMap.isEmpty()) {
            log(Level.DEBUG, "Tabs to add: %s", customTabMap.values());
            for(Tab t: customTabMap.values()) {
                t.setHostlist(this);
                GraphTree tabtree = t.getGraphTree();
                if(tabtree != null)
                    treeMap.put(t.getName(), tabtree);
            }
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
     * 
     * @param label The name of the tree
     * @param root The name of the first element in the tree
     * @return a graph tree
     */
    private GraphTree addTree(String label, String root) {
        if(!treeMap.containsKey(label)) {
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
     * 
     * @param gn
     * @param pathList
     */
    private void checkRoles(GraphNode gn, String root, List<String> pathList) {
        StringBuilder path = new StringBuilder("/" + root);
        for(String pathElem: pathList) {
            path.append('/').append(pathElem);
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
     * 
     * @param id the hash value of the graph
     * @return the graph found or null of nothing found
     */
    public GraphNode getGraphById(int id) {
        return graphMap.get(id);
    }

    /**
     * Return a probe identified by his hash value
     * 
     * @param id the hash value of the probe
     * @return the probe found or null of nothing found
     */
    public Probe<?, ?> getProbeById(int id) {
        return probeMap.get(id);
    }

    /**
     * Return a probe identified by path
     * 
     * @param host the host
     * @param probeName the probe name: the probeName element of a probedesc
     * @return the graph found or null of nothing found
     */
    public Probe<?, ?> getProbeByPath(String host, String probeName) {
        String path = host + "/" + probeName;
        return probeMap.get(path.hashCode());
    }

    public void addProbe(Probe<?, ?> p) {
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
                daSet.add(daa.getConstructor().newInstance());
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

    @SuppressWarnings("unchecked")
    @Override
    public Stream<jrds.starter.Timer> getChildsStream() {
        return timers.values().stream();
    }

    @Override
    public void stopCollect() {
        super.stopCollect();
        Optional.ofNullable(collectTimer).ifPresent(Timer::cancel);
        collectTimer = null;
        // Everything is stopped, wait for collect termination
        try {
            timers.values().forEach(t -> {
                try {
                    t.lockCollect();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CancellationException();
                }
            });
            timers.values().forEach(jrds.starter.Timer::releaseCollect);
        } catch (CancellationException e) {
            // The interrupt was already handled
        }
        Optional.ofNullable(renderer).ifPresent(Renderer::finish);
        stores.forEach(StoreFactory::stop);
        ClassLoader probeClassLoader = probeClassResolver.getClassLoader();
        try {
            if (probeClassLoader instanceof URLClassLoader && ! probeClassLoader.equals(getClass().getClassLoader())) {
                ((URLClassLoader)probeClassLoader).close();
            }
        } catch (IOException ex) {
            log(Level.ERROR, ex, "Failed to stop probes class loader: {}", Util.resolveThrowableException(ex));
        }
    }

    @Override
    public Logger getInstanceLogger() {
        return logger;
    }

}
