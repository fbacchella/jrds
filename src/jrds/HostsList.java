/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jrds.configuration.ConfigObjectFactory;
import jrds.factories.ProbeMeta;
import jrds.probe.ContainerProbe;
import jrds.probe.SumProbe;
import jrds.probe.VirtualProbe;
import jrds.starter.Starter;
import jrds.starter.StarterNode;
import jrds.webapp.ACL;
import jrds.webapp.DiscoverAgent;
import jrds.webapp.RolesACL;

import org.apache.log4j.Logger;

/**
 * The central repository of all informations : hosts, graph, and everything else
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class HostsList extends StarterNode {
    static private final Logger logger = Logger.getLogger(HostsList.class);

    public static final class Stats {
        Stats() {
            lastCollect = new Date(0);
        }
        public long runtime = 0;
        public Date lastCollect;
    }

    private RdsHost sumhost =  null;
    private RdsHost customhost =  null;

    private final Set<RdsHost> hostList = new HashSet<RdsHost>();
    private final Map<Integer, GraphNode> graphMap = new HashMap<Integer, GraphNode>();
    private final Map<Integer, Probe<?,?>> probeMap= new HashMap<Integer, Probe<?,?>>();
    private final Map<String, GraphTree> treeMap = new LinkedHashMap<String, GraphTree>(3);
    private final Map<String, Filter> filters = new TreeMap<String, Filter>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Tab> tabs = new LinkedHashMap<String, Tab>();
    private String firstTab = null;
    private Renderer renderer = null;
    private int numCollectors = 1;
    private int step;
    private File rrdDir = null;
    private File tmpDir = null;
    private int timeout = 10;
    // The list of roles known to jrds
    private Set<String> roles = new HashSet<String>();
    private Set<String> defaultRoles = Collections.emptySet();
    // A global flag that tells globally that this HostsList can be used
    volatile private boolean started = false;
    private Stats stats = new Stats();
    private Set<Class<? extends DiscoverAgent>> daList = new HashSet<Class<? extends DiscoverAgent>>();

    private Semaphore collectMutex = new Semaphore(1);

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
        addTree(Filter.SUM.getName(), GraphTree.SUMROOT);
        filters.put(Filter.SUM.getName(), Filter.SUM);
        sumhost =  new RdsHost("SumHost");

        addTree(Filter.CUSTOM.getName(), Filter.CUSTOM.getName());
        filters.put(Filter.CUSTOM.getName(), Filter.CUSTOM);
        customhost =  new RdsHost("CustomHost");

        filters.put(Filter.EVERYTHING.getName(), Filter.EVERYTHING);

        addTree(Filter.ALLHOSTS.getName(), GraphTree.HOSTROOT);
        filters.put(Filter.ALLHOSTS.getName(), Filter.ALLHOSTS);

        GraphTree viewtree = addTree(Filter.ALLVIEWS.getName(), GraphTree.VIEWROOT);
        viewtree.addPath("Services");
        filters.put(Filter.ALLVIEWS.getName(), Filter.ALLVIEWS);

        treeMap.put(Filter.ALLSERVICES.getName(), Filter.ALLSERVICES.setRoot(viewtree));
        filters.put(Filter.ALLSERVICES.getName(), Filter.ALLSERVICES);

        addTree("All tags", GraphTree.TAGSROOT);

        sumhost.setParent(this);
        customhost.setParent(this);
    }

    public void configure(PropertiesManager pm) {
        started = false;
        try {
            jrds.JrdsLoggerConfiguration.configure(pm);
        } catch (IOException e1) {
            logger.error("Unable to set log file to " + pm.logfile);
        }

        if(pm.rrddir == null) {
            logger.error("Probes directory not configured, can't configure");
            return;
        }

        if(pm.configdir == null) {
            logger.error("Configuration directory not configured, can't configure");
            return;
        }

        numCollectors = pm.collectorThreads;
        step = pm.step;
        started = true;
        rrdDir = pm.rrddir;
        tmpDir = pm.tmpdir;

        renderer = new Renderer(50, step, tmpDir);

        logger.debug("Starting parsing descriptions");
        ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);
        conf.setGraphDescMap();
        Collection<ProbeDesc> probesdesc = conf.setProbeDescMap().values();

        Set<Class<? extends Starter>> externalStarters = new HashSet<Class<? extends Starter>>();
        for(ProbeDesc pd: probesdesc) {
            Class<?> pc = pd.getProbeClass();
            while(pc != null && pc != StarterNode.class) {
                if(pc.isAnnotationPresent(ProbeMeta.class)) {
                    ProbeMeta meta = pc.getAnnotation(ProbeMeta.class);
                    daList.add(meta.discoverAgent());
                    externalStarters.add(meta.topStarter());
                }
                pc = pc.getSuperclass();
            }
        }
        conf.setMacroMap();
        conf.setTabMap();

        Set<String> hostsTags = new HashSet<String>();
        Map<String, RdsHost> hosts = conf.setHostMap();
        for(RdsHost h: hosts.values()) {
            addHost(h);
            h.configureStarters(pm);
            for(Probe<?,?> p: h.getProbes()) {
                p.setTimeout(getTimeout());
                addProbe(p);
                p.configureStarters(pm);
                for(String hostTag: p.getTags()) {
                    hostsTags.add(hostTag);
                }
            }				
        }

        //We try to load top level starter defined in probes
        logger.debug(jrds.Util.delayedFormatString("External top starters added %s", externalStarters));
        for(Class<? extends Starter> starterClass: externalStarters) {
            try {
                registerStarter(starterClass.newInstance());
            } catch (Exception e) {
                logger.error("Starter " + starterClass + " failed to register:" + e, e);
            }
        }
        configureStarters(pm);

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

        //Let's build the tab with all the sums
        Tab sumsTab = null;
        Map<String, SumProbe> sums = conf.setSumMap();
        if(sums.size() > 0) {
            sumsTab = new Tab.DynamicTree("All sums", PropertiesManager.SUMSTAB);
            for(SumProbe s: sums.values()) {
                addVirtual(s, sumhost, Filter.SUM.getName());
                GraphNode sum = s.getGraphList().iterator().next();
                //String id = Integer.toString(sum.hashCode());
                graphMap.put(sum.getQualifieName().hashCode(), sum);
                sumsTab.add(sum.getQualifieName(), Collections.singletonList(s.getName()));
            }
            allTabs.add(sumsTab);
        }

        //Let's build all the custom tabs
        Map<String, Tab> customTabMap = conf.setTabMap();
        logger.debug(jrds.Util.delayedFormatString("Tabs to add: %s", customTabMap.values()));
        for(Tab t: customTabMap.values()) {
            t.setHostlist(this);
            GraphTree tabtree = t.getGraphTree();
            if(tabtree != null)
                treeMap.put(t.getName(), tabtree);
            allTabs.add(t);
        }

        logger.debug("Parsing graphs configuration");
        Map<String, GraphDesc> graphs = conf.setGrapMap();
        //Let's build the tab with all the custom graphs
        Tab customGraphsTab = null;
        if(! graphs.isEmpty()) {
            customGraphsTab = new Tab.DynamicTree("Custom graphs", PropertiesManager.CUSTOMGRAPHTAB);
            ContainerProbe cp = new ContainerProbe(customhost.getName());
            for(GraphDesc gd: graphs.values()) {
                logger.trace("Adding graph: " + gd.getGraphTitle());
                cp.addGraph(gd);
            }
            addVirtual(cp, customhost, Filter.CUSTOM.getName());
            for(GraphNode gn: cp.getGraphList()) {
                customGraphsTab.add(gn.getQualifieName(), gn.getGraphDesc().getHostTree(gn));
            }
            allTabs.add(customGraphsTab);
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
        logger.trace(jrds.Util.delayedFormatString("Looking for tabs list %s in %s", pm.tabsList, moretabs));
        for(String tabid: pm.tabsList) {
            if("@".equals(tabid)) {
                for(Tab t: customTabMap.values()) {
                    tabs.put(t.getId(), t);
                }
            }
            else {
                Tab t = tabsmap.get(tabid);
                if(t == null) {
                    logger.error("Non existent tab to add: " + tabid);
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

    public Collection<RdsHost> getHosts() {
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
                logger.trace(jrds.Util.delayedFormatString("Adding ACL %s to %s", f.getACL(), gn));
                gn.addACL(f.getACL());
            }
        }
    }

    public void addHost(RdsHost newhost) {
        hostList.add(newhost);
        newhost.setParent(this);
    }

    public void collectAll() {
        if(started) {
            logger.debug("One collect is launched");
            Date start = new Date();
            try {
                if( ! collectMutex.tryAcquire(getTimeout(), TimeUnit.SECONDS)) {
                    logger.fatal("A collect failed because a start time out");
                    return;
                }
            } catch (InterruptedException e) {
                logger.fatal("A collect start was interrupted");
                return;
            }
            try {
                final Object counter = new Object() {
                    int i = 0;
                    @Override
                    public String toString() {
                        return Integer.toString(i++);
                    }

                };
                ExecutorService tpool =  Executors.newFixedThreadPool(numCollectors, 
                        new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "CollectorThread" + counter);
                        t.setDaemon(true);
                        logger.debug("New thread name:" + t.getName());
                        return t;
                    }
                }
                        );
                startCollect();
                for(final RdsHost oneHost: hostList) {
                    if( ! isCollectRunning())
                        break;
                    logger.debug("Collect all stats for host " + oneHost.getName());
                    Runnable runCollect = new Runnable() {
                        private RdsHost host = oneHost;

                        public void run() {
                            Thread.currentThread().setName("JrdsCollect-" + host.getName());
                            host.collectAll();
                            Thread.currentThread().setName("JrdsCollect-" + host.getName() + ":finished");
                        }
                        @Override
                        public String toString() {
                            return Thread.currentThread().toString();
                        }
                    };
                    try {
                        tpool.execute(runCollect);
                    }
                    catch(RejectedExecutionException ex) {
                        logger.debug("collector thread dropped for host " + oneHost.getName());
                    }
                }
                tpool.shutdown();
                try {
                    tpool.awaitTermination(step - getTimeout() * 2 , TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.warn("Collect interrupted");
                }
                stopCollect();
                if( ! tpool.isTerminated()) {
                    //Second chance, we wait for the time out
                    boolean emergencystop = false;
                    try {
                        emergencystop = tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.warn("Collect interrupted in last chance");
                    }
                    if(! emergencystop) {
                        //logger.info("Some probes are hanged");

                        //					if(! emergencystop) {
                        logger.warn("Some task still alive, needs to be killed");
                        //						//Last chance to commit results
                        List<Runnable> timedOut = tpool.shutdownNow();
                        if(! timedOut.isEmpty()) {
                            logger.warn("Still " + timedOut.size() + " waiting probes: ");
                            for(Runnable r: timedOut) {
                                logger.warn(r.toString());
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                logger.error("problem while collecting data: ", e);
            }
            finally {
                //StoreOpener.
                collectMutex.release();				
            }
            Date end = new Date();
            long duration = end.getTime() - start.getTime();
            synchronized(stats) {
                stats.lastCollect = start;
                stats.runtime = duration;
            }
            System.gc();
            logger.info("Collect started at "  + start + " ran for " + duration + "ms");
        }
    }

    public void lockCollect() throws InterruptedException {
        collectMutex.acquire();
    }

    public void releaseCollect() {
        collectMutex.release();
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

    public void addFilter(Filter newFilter) {
        filters.put(newFilter.getName(), newFilter);
        ACL acl = newFilter.getACL();
        if(acl instanceof RolesACL) {
            roles.addAll(((RolesACL) acl).getRoles());
        }
        logger.debug(jrds.Util.delayedFormatString("Filter %s added with ACL %s", newFilter.getName(), newFilter.getACL()));
    }

    public Filter getFilter(String name) {
        Filter retValue = null;
        if(name != null)
            retValue = filters.get(name);
        return retValue;
    }

    public Collection<String> getAllFiltersNames() {
        return filters.keySet();
    }

    private void addVirtual(VirtualProbe vprobe, RdsHost vhost, String root) {
        try {
            vhost.getProbes().add(vprobe);
            vprobe.setHost(vhost);
            for(GraphNode currGraph: vprobe.getGraphList()) {
                logger.trace("adding virtual graph: " + currGraph);
                treeMap.get(root).addGraphByPath(currGraph.getTreePathByHost(), currGraph);
                graphMap.put(currGraph.hashCode(), currGraph);
            }
            logger.debug("adding virtual probe " + vprobe.getName());
        } catch (Exception e) {
            logger.error("Virtual probe initialization failed for " + vhost.getName() + "/" + vprobe.getName(), e);
        }
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

    public int getStep() {
        return step;
    }

    public File getRrdDir() {
        return rrdDir;
    }

    public File getTmpdir() {
        return tmpDir;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * @return the stats
     */
    public Stats getStats() {
        return stats;
    }

    /**
     * @param started the started to set
     */
    public void finished() {
        this.started = false;
    }

    /* (non-Javadoc)
     * @see jrds.starter.StarterNode#isCollectRunning()
     */
    @Override
    public boolean isCollectRunning() {
        return started && super.isCollectRunning();
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
                logger.error("Error creating discover agent " + daa.getName() + ": " + e, e);
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
}