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

import jrds.snmp.SnmpRequester;

import org.apache.log4j.Logger;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * The classe used to store all the host to manage
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class HostsList {
	static Logger logger = Logger.getLogger(HostsList.class);
	private Collection hostList;
	private static HostsList instance;
	private GraphTree graphTreeByHost = null;
	private GraphTree graphTreeByView = null;
	private Map graphMap;
	private Map probeMap;
	private static final String hostRoot = "Sorted by host";
	private static final String viewRoot = "Sorted by view";
	private Map macroList = new HashMap();
	private Map treeMap = null;

	/**
	 *  
	 */
	private HostsList() {
		init();
	}

	private void init() {
		treeMap = new HashMap(2);
		graphTreeByHost = GraphTree.makeGraph(hostRoot);
		graphTreeByView = GraphTree.makeGraph(viewRoot);
		treeMap.put(hostRoot, graphTreeByHost);
		treeMap.put(viewRoot, graphTreeByView);
		graphMap = new HashMap();
		probeMap = new HashMap();
		hostList = new HashSet();
	}
	public static HostsList getRootGroup() {
		if (instance == null)
			instance = new HostsList();
		return instance;
	}
	
	public Iterator iterator() {
		return hostList.iterator();
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

	private void addHost(RdsHost newhost) {
		hostList.add(newhost);
		for (Iterator j = newhost.getProbes().iterator(); j.hasNext();) {
			Probe currProbe = (Probe) j.next();
			probeMap.put(new Integer(currProbe.hashCode()), currProbe);
			if (currProbe.getGraphList() != null)
				for (Iterator k = currProbe.getGraphList().iterator(); k
						.hasNext();) {
					RdsGraph currGraph = (RdsGraph) k.next();
					graphTreeByView.addGraphByPath(currGraph
							.getTreePathByView(), currGraph);
					graphTreeByHost.addGraphByPath(currGraph
							.getTreePathByHost(), currGraph);
					graphMap.put(new Integer(currGraph.hashCode()), currGraph);
				}
		}
		if (this != instance)
			instance.addHost(newhost);
	}

	public void collectAll() throws IOException {
		SnmpRequester.start();
		ExecutorService tpool =  Executors.newFixedThreadPool(PropertiesManager.getInstance().collectorThreads);
		logger.debug("One collect was launched");
		Date start = new Date();
		for (Iterator j = hostList.iterator(); j.hasNext();) {
			final RdsHost oneHost = (RdsHost) j.next();
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
			tpool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			logger.info("Collect interrupted");
		}
		Date end = new Date();
		long duration = end.getTime() - start.getTime();
		logger.info("Collect started at "  + start + " ran for " + duration + "ms");							
		SnmpRequester.stop();
	}
	
	public void graphAll(Date startDate, Date endDate) {
		for (Iterator j = hostList.iterator(); j.hasNext();) {
			RdsHost oneHost = (RdsHost) j.next();
			logger.debug("Do all graph for host " + oneHost.getName());
			oneHost.graphAll(startDate, endDate);
		}
	}

	public GraphTree getGraphTreeByHost() {
		return graphTreeByHost;
	}

	public GraphTree getGraphTreeByView() {
		return graphTreeByView;
	}

	/**
	 * Return a graph identified by his hash value
	 * @param id the hash value of the graph
	 * @return the graph found or null of nothing found
	 */
	public RdsGraph getGraphById(int id) {
		return (RdsGraph) graphMap.get(new Integer(id));
	}

	/**
	 * Return a probe identified by his hash value
	 * @param id the hash value of the probe
	 * @return the graph found or null of nothing found
	 */
	public Probe getProbeById(int id) {
		return (Probe) probeMap.get(new Integer(id));
	}

	public GraphTree getNodeByPath(String path) {
		GraphTree tree = (GraphTree) treeMap.get(path.split("/")[1]);
		GraphTree node = null;
		if(tree != null) {
			node = tree.getByPath(path);
		}
		return node;
	}

	/**
	 * @return
	 */
	public Map getMacroList() {
		return macroList;
	}

	/**
	 * @param macroList
	 */
	public void setMacroList(Map macroList) {
		this.macroList = macroList;
	}
}