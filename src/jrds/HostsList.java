/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * The classe used to store all the host to manage
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class HostsList {
	static Logger logger = Logger.getLogger(HostsList.class);
	private Collection<RdsHost> hostList;
	private static HostsList instance;
	private Map<Integer, RdsGraph> graphMap;
	private Map<Integer, Probe> probeMap;
	private static final String hostRoot = "Sorted by host";
	private static final String viewRoot = "Sorted by view";
	private Map<String, Macro> macroList = new HashMap<String, Macro>();
	private Map<String, GraphTree> treeMap = null;
	private StartersSet starters = new StartersSet();

	/**
	 *  
	 */
	private HostsList() {
		init();
	}

	private void init() {
		treeMap = new HashMap<String, GraphTree>(2);
		addRoot(hostRoot);
		addRoot(viewRoot);
		graphMap = new HashMap<Integer, RdsGraph>();
		probeMap = new HashMap<Integer, Probe>();
		hostList = new HashSet<RdsHost>();
	}
	
	public static HostsList getRootGroup() {
		if (instance == null)
			instance = new HostsList();
		return instance;
	}
	
	public Iterator iterator() {
		return hostList.iterator();
	}
	
	public static void purge() {
		instance = new HostsList();
	}
	
	public static HostsList fill(File newHostCfgFile) {
		instance = new HostsList();
		instance.append(newHostCfgFile);
		return instance;
	}

	public void append(Collection newHostList) {
		for (Iterator i = newHostList.iterator(); i.hasNext();) {
			addHost((RdsHost) i.next());
		}
	}

	public void append(File newHostCfgFile) {
		HostConfigParser aparser = new HostConfigParser(newHostCfgFile);
		Collection tmpHostsList = aparser.parse();
		append(tmpHostsList);
	}

	private void addRoot(String root) {
		if( ! treeMap.containsKey(root)) {
			 GraphTree newRoot = GraphTree.makeGraph(root);
			 treeMap.put(root, newRoot);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addGraph(String root, RdsGraph graph) {
		getGraphTreeByHost().addGraphByPath(graph.getTreePathByHost(), graph);
		getGraphTreeByView().addGraphByPath(graph.getTreePathByView(), graph);
	}
	
	public void addHost(RdsHost newhost) {
		hostList.add(newhost);
		for (Iterator j = newhost.getProbes().iterator(); j.hasNext();) {
			Probe currProbe = (Probe) j.next();
			probeMap.put(currProbe.hashCode(), currProbe);
			if (currProbe.getGraphList() != null) {
				String rootTree = currProbe.getTree();
				if(rootTree != null)
					addRoot(rootTree);
				for(RdsGraph currGraph: currProbe.getGraphList()) {
					addGraph(rootTree, currGraph);
					graphMap.put(currGraph.hashCode(), currGraph);
				}
			}
		}
		if (this != instance)
			instance.addHost(newhost);
	}

	public void collectAll() throws IOException {
		starters.startCollect();
		ExecutorService tpool =  Executors.newFixedThreadPool(PropertiesManager.getInstance().collectorThreads);
		logger.debug("One collect was launched");
		Date start = new Date();
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
			tpool.awaitTermination(PropertiesManager.getInstance().resolution - 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.info("Collect interrupted");
		}
		Date end = new Date();
		long duration = end.getTime() - start.getTime();
		logger.info("Collect started at "  + start + " ran for " + duration + "ms");							
		starters.stopCollect();
	}
	
	public void graphAll(Date startDate, Date endDate) {
		for(RdsHost oneHost: hostList) {
			logger.debug("Do all graph for host " + oneHost.getName());
			oneHost.graphAll(startDate, endDate);
		}
	}

	public GraphTree getGraphTreeByHost() {
		return treeMap.get(hostRoot);
	}

	public GraphTree getGraphTreeByView() {
		return treeMap.get(viewRoot);
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

	public GraphTree getNodeByPath(String path) {
		GraphTree tree = treeMap.get(path.split("/")[1]);
		GraphTree node = null;
		if(tree != null) {
			node = tree.getByPath(path);
		}
		return node;
	}

	public GraphTree getNodeById(int id) {
		GraphTree node = null;
		for(GraphTree tree: treeMap.values())
			if(tree.getById(id) != null)
				node = tree.getById(id);
		return node;
	}

	/**
	 * @return
	 */
	public Map<String, Macro> getMacroList() {
		return macroList;
	}

	/**
	 * @param macroList
	 */
	public void setMacroList(Map<String, Macro> macroList) {
		this.macroList = macroList;
	}
	
	public void addStarter(Starter s) {
		starters.register(s);
	}
}