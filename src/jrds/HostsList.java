/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jrds.probe.SumProbe;

import org.apache.log4j.Logger;

/**
 * The central repository of all informations : hosts, graph, and everything else
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class HostsList {
	static private final Logger logger = Logger.getLogger(HostsList.class);
	private static HostsList instance;

	public static final String HOSTROOT = "Sorted by host";
	public static final String VIEWROOT = "Sorted by view";
	public static final String SUMROOT = "Sums";

	private StartersSet starters = null;
	private RdsHost sumhost =  null;

	private final Set<RdsHost> hostList = new HashSet<RdsHost>();
	private final Map<Integer, RdsGraph> graphMap = new HashMap<Integer, RdsGraph>();
	private final Map<Integer, Probe> probeMap= new HashMap<Integer, Probe>();
	private final Map<String, GraphTree> treeMap = new LinkedHashMap<String, GraphTree>(3);
	private final Map<String, Filter> filters = new TreeMap<String, Filter>(String.CASE_INSENSITIVE_ORDER);
	private final Renderer renderer = new Renderer(20);
	GraphFactory gf;
	ProbeFactory pf;
	private int numCollectors = 1;
	private int resolution;
	private String rrdDir;
	private String tmpdir;
	private int timeout;
	private boolean started = false;

	/**
	 *  
	 */
	private HostsList() {
		instance =  this;
		init();
	}

	private void init() {
		addRoot(HOSTROOT);
		addRoot(VIEWROOT);
		addRoot(SUMROOT);
		filters.put(Filter.SUM.getName(), Filter.SUM);
		filters.put(Filter.EVERYTHING.getName(), Filter.EVERYTHING);
		filters.put(Filter.ALLHOSTS.getName(), Filter.ALLHOSTS);
		filters.put(Filter.ALLVIEWS.getName(), Filter.ALLVIEWS);
		filters.put(Filter.ALLSERVICES.getName(), Filter.ALLSERVICES);
		starters = new StartersSet(this);
		sumhost =  new RdsHost("SumHost");
	}

	public static HostsList getRootGroup() {
		if (instance == null)
			new HostsList();
		return instance;
	}

	public void configure(PropertiesManager pm) {
		started = false;
		try {
			jrds.JrdsLoggerConfiguration.configure(pm);
		} catch (IOException e1) {
			logger.error("Unable to set log file to " + pm.logfile);
		}

		numCollectors = pm.collectorThreads;
		resolution = pm.resolution;
		rrdDir = pm.rrddir;
		tmpdir = pm.tmpdir;

		DescFactory df = new DescFactory();

		try {
			df.importDescUrl(DescFactory.class.getResource("/probe"));
			df.importDescUrl(DescFactory.class.getResource("/graph"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(! "".equals(pm.libspath)) {
			String[] libsPath = pm.libspath.split(";");
			for(int i=0 ; i < libsPath.length ; i++) {
				String lib = libsPath[i];
				try {
					logger.info("Adding lib " + lib);
					if(lib.endsWith(".jar"))
						df.importJar(lib);
					else
						df.importDir(new File(lib));
				} catch (IOException e) {
					logger.error("Lib " + lib + " can't be imported: " + e);
				}
			}
		}

		gf = new GraphFactory(df.getGraphDescMap(), pm.legacymode);
		pf = new ProbeFactory(df.getProbesDesc(), gf, pm, pm.legacymode);
		HostConfigParser hp = new HostConfigParser(pf);

		List<String> graphsName = new ArrayList<String>(df.getGraphDescMap().keySet());
		logger.debug("Graphs :" + graphsName);
		List<String> probesName = new ArrayList<String>(df.getProbesDesc().keySet());
		Collections.sort(probesName);
		logger.debug("Probes: " + probesName);
		if(pm.configdir != null)
			hp.importDir(new File(pm.configdir,"/macro"));
		if(pm.configdir != null)
			hp.importDir(new File(pm.configdir));
		started = true;
	}

	public Collection<RdsHost> getHosts() {
		return hostList;

	}

	public static void purge() {
		instance.started = false;
		StoreOpener.reset();
		instance.renderer.finish();
		instance = new HostsList();
	}

	private void addRoot(String root) {
		if( ! treeMap.containsKey(root)) {
			GraphTree newRoot = GraphTree.makeGraph(root);
			treeMap.put(root, newRoot);
		}
	}

	public void addGraphs(Collection<RdsGraph> graphs) {
		for(RdsGraph currGraph: graphs) {
			getGraphTreeByHost().addGraphByPath(currGraph.getTreePathByHost(), currGraph);
			getGraphTreeByView().addGraphByPath(currGraph.getTreePathByView(), currGraph);
			graphMap.put(currGraph.hashCode(), currGraph);
		}
	}

	public void addHost(RdsHost newhost) {
		hostList.add(newhost);
	}

	public void collectAll() {
		logger.debug("One collect was launched");
		Date start = new Date();
		try {
		starters.startCollect();
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
		for(final RdsHost oneHost: hostList) {
			logger.debug("Collect all stats for host " + oneHost.getName());
			Runnable runCollect = new Runnable() {
				private RdsHost host = oneHost;

				public void run() {
					host.collectAll();
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
			tpool.awaitTermination(resolution - 60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Collect interrupted");
		}
		starters.stopCollect();
        if( ! tpool.isTerminated()) {
        	List<Runnable> timedOut = tpool.shutdownNow();
            logger.warn("Still " + timedOut.size() + " waiting probes: ");
            for(Runnable r: timedOut) {
            	logger.warn(r.toString());
             }
        }
		} catch (RuntimeException e) {
			logger.error("problem while collecting data: ", e);
		}							
		Date end = new Date();
		long duration = end.getTime() - start.getTime();
		System.gc();
		logger.info("Collect started at "  + start + " ran for " + duration + "ms");							
	}

	public void graphAll(Date startDate, Date endDate) {
		for(RdsHost oneHost: hostList) {
			logger.debug("Do all graph for host " + oneHost.getName());
			oneHost.graphAll(startDate, endDate);
		}
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
	public RdsGraph getGraphById(int id) {
		return graphMap.get(id);
	}

	/**
	 * Return a probe identified by his hash value
	 * @param id the hash value of the probe
	 * @return the graph found or null of nothing found
	 */
	public Probe getProbeById(int id) {
		return probeMap.get(id);
	}

	public void addProbe(Probe p) {
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

	public void addStarter(Starter s) {
		starters.registerStarter(s, this);
	}

	public StartersSet getStarters() {
		return starters;
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

	public void addSum(SumProbe sum) {
		sumhost.addProbe(sum);
		for(RdsGraph currGraph: sum.getGraphList()) {
			treeMap.get(SUMROOT).addGraphByPath(currGraph.getTreePathByHost(), currGraph);
			graphMap.put(currGraph.hashCode(), currGraph);
		}
		logger.debug("adding sum " + sum.getName());
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

	public int getResolution() {
		return resolution;
	}

	public String getRrdDir() {
		return rrdDir;
	}

	public String getTmpdir() {
		return tmpdir;
	}

	public void setTmpdir(String tmpdir) {
		this.tmpdir = tmpdir;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isStarted() {
		return started;
	}

	public GraphFactory getGraphFactory() {
		return gf;
	}

	public ProbeFactory getProbeFactory() {
		return pf;
	}

}
