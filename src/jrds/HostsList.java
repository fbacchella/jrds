/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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

import javax.xml.parsers.ParserConfigurationException;

import jrds.factories.ConfigObjectFactory;
import jrds.factories.Loader;
import jrds.probe.ContainerProbe;
import jrds.probe.SumProbe;
import jrds.probe.VirtualProbe;
import jrds.starter.SocketFactory;
import jrds.starter.StarterNode;

import org.apache.log4j.Logger;

/**
 * The central repository of all informations : hosts, graph, and everything else
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class HostsList extends StarterNode {
	static private final Logger logger = Logger.getLogger(HostsList.class);

	public class Stats {
		Stats() {
			lastCollect = new Date(0);
		}
		public long runtime = 0;
		public Date lastCollect;
	}

	public static final String HOSTROOT = "Sorted by host";
	public static final String VIEWROOT = "Sorted by view";
	public static final String SUMROOT = "Sums";
	public static final String CUSTOMROOT = "Dashboard";
	public static final String TAGSROOT = "All tags";

	private RdsHost sumhost =  null;
	private RdsHost customhost =  null;

	private final Set<RdsHost> hostList = new HashSet<RdsHost>();
	private final Map<Integer, GraphNode> graphMap = new HashMap<Integer, GraphNode>();
	private final Map<Integer, Probe<?,?>> probeMap= new HashMap<Integer, Probe<?,?>>();
	private final Map<String, GraphTree> treeMap = new LinkedHashMap<String, GraphTree>(3);
	private final Map<String, Filter> filters = new TreeMap<String, Filter>(String.CASE_INSENSITIVE_ORDER);
	private Renderer renderer = null;
	private int numCollectors = 1;
	private int step;
	private File rrdDir = null;
	private File tmpDir = null;
	private int timeout = 10;
	private Set<String> roles = new HashSet<String>();
	private Set<String> defaultRoles = Collections.emptySet();
	// A global flag that tells globally that this HostsList can be used
	volatile private boolean started = false;
	private Stats stats = new Stats();

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
		init();
		configure(pm);
	}

	private void init() {
		addRoot(SUMROOT);
		filters.put(Filter.SUM.getName(), Filter.SUM);
		sumhost =  new RdsHost("SumHost");

		addRoot(CUSTOMROOT);
		filters.put(Filter.CUSTOM.getName(), Filter.CUSTOM);
		customhost =  new RdsHost("CustomHost");

		filters.put(Filter.EVERYTHING.getName(), Filter.EVERYTHING);

		addRoot(HOSTROOT);
		filters.put(Filter.ALLHOSTS.getName(), Filter.ALLHOSTS);

		addRoot(VIEWROOT);
		filters.put(Filter.ALLVIEWS.getName(), Filter.ALLVIEWS);

		filters.put(Filter.ALLSERVICES.getName(), Filter.ALLSERVICES);

		addRoot(TAGSROOT);

		jrds.snmp.SnmpStarter.full.register(this);
		new SocketFactory().register(this);
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

		if(pm.security) {
			defaultRoles = pm.defaultRoles;
			roles.addAll(defaultRoles);
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

		find(SocketFactory.class).setTimeout(pm.timeout);

		renderer = new Renderer(50, step, tmpDir);

		Loader l;
		try {
			l = new Loader();
			URL graphUrl = getClass().getResource("/desc");
			if(graphUrl != null)
				l.importUrl(graphUrl);
			else {
				logger.fatal("Default probes not found");
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Loader initialisation error",e);
		}

		logger.debug("Scanning " + pm.libspath + " for probes libraries");
		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			l.importUrl(lib);
		}

		l.importDir(pm.configdir);

		logger.debug("Starting parsing descriptions");
		ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);

		conf.setGraphDescMap(l.getRepository(Loader.ConfigType.GRAPHDESC));
		conf.setProbeDescMap(l.getRepository(Loader.ConfigType.PROBEDESC));
		conf.setMacroMap(l.getRepository(Loader.ConfigType.MACRODEF));

		Set<String> hostsTags = new HashSet<String>();
		Map<String, RdsHost> hosts = conf.setHostMap(l.getRepository(Loader.ConfigType.HOSTS));
		for(RdsHost h: hosts.values()) {
			addHost(h);
			for(Probe<?,?> p: h.getProbes()) {
				p.setTimeout(getTimeout());
				addProbe(p);
				for(String hostTag: p.getTags()) {
					hostsTags.add(hostTag);
				}
			}				
		}

		for(String tag: hostsTags) {
			Filter f = new FilterTag(tag);
			filters.put(f.getName(), f);
		}

		Map <String, Filter> f = conf.setFilterMap(l.getRepository(Loader.ConfigType.FILTER));
		for(Filter filter: f.values()) {
			addFilter(filter);
		}

		Map<String, SumProbe> sums = conf.setSumMap(l.getRepository(Loader.ConfigType.SUM));
		for(SumProbe s: sums.values()) {
			addVirtual(s, sumhost, SUMROOT);
		}

		logger.debug("Parsing graphs configuration");
		Map<String, GraphDesc> graphs = conf.setGrapMap(l.getRepository(Loader.ConfigType.GRAPH));
		if(! graphs.isEmpty()) {
			ContainerProbe cp = new ContainerProbe(customhost.getName());
			for(GraphDesc gd: graphs.values()) {
				logger.trace("Adding graph: " + gd.getGraphTitle());
				cp.addGraph(gd);
			}
			addVirtual(cp, customhost, CUSTOMROOT);
		}
		started = true;
	}

	public Collection<RdsHost> getHosts() {
		return hostList;

	}

	private void addRoot(String root) {
		if( ! treeMap.containsKey(root)) {
			GraphTree newRoot = GraphTree.makeGraph(root);
			treeMap.put(root, newRoot);
		}
	}

	public void addGraphs(Collection<GraphNode> graphs) {
		for(GraphNode currGraph: graphs) {
			LinkedList<String> path;

			path = currGraph.getTreePathByHost();
			getGraphTreeByHost().addGraphByPath(path, currGraph);
			checkRoles(currGraph, path);

			path = currGraph.getTreePathByView();
			getGraphTreeByView().addGraphByPath(path, currGraph);
			checkRoles(currGraph, path);

			graphMap.put(currGraph.hashCode(), currGraph);
		}
	}

	private void checkRoles(GraphNode gn, List<String> pathList) {
		StringBuilder path = new StringBuilder();
		for(String pathElem: pathList) {
			path.append("/").append(pathElem);
		}
		for(Filter f: filters.values()) {
			if(f.acceptGraph(gn, path.toString())) {
				gn.addRoles(f.getRoles());
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
				collectMutex.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException("Collect task failed to start", e);
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
					logger.info("Collect interrupted");
				}
				stopCollect();
				if( ! tpool.isTerminated()) {
					//Second chance, we wait for the time out
					boolean emergencystop = false;
					try {
						emergencystop = tpool.awaitTermination(getTimeout(), TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						logger.info("Collect interrupted in last chance");
					}
					if(! emergencystop) {
						logger.info("Some task still alive, needs to be killed");
						//Last chance to commit results
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


	public Collection<GraphTree> getGraphsRoot() {
		return treeMap.values();
	}

	public GraphTree getGraphTreeByHost() {
		return treeMap.get(HOSTROOT);
	}

	public GraphTree getGraphTreeByView() {
		return treeMap.get(VIEWROOT);
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
		for(GraphTree tree: treeMap.values())
			if(tree.getById(id) != null)
				node = tree.getById(id);
		return node;
	}

	public void addFilter(Filter newFilter) {
		filters.put(newFilter.getName(), newFilter);
		logger.debug("Filter " + newFilter.getName() + " added");
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

}