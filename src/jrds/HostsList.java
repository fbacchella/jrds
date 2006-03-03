/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
	private GraphTreeNode graphTreeByHost = null;
	private GraphTreeNode graphTreeByView = null;
	private Map graphMap;
	private static final String hostRoot = "Sorted by host";
	private static final String viewRoot = "Sorted by view";
	private Map macroList = new HashMap();

	/**
	 *  
	 */
	private HostsList() {
		init();
	}

	private void init() {
		graphTreeByHost = new GraphTreeNode(hostRoot);
		graphTreeByView = new GraphTreeNode(viewRoot);
		graphMap = new HashMap();
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

	public Collection enumProbes() {
		Collection allrrds = new HashSet(hostList.size() * 10);
		for (Iterator i = hostList.iterator(); i.hasNext();) {
			RdsHost oneHost = (RdsHost) i.next();
			allrrds.addAll(oneHost.getProbes());
		}
		return allrrds;
	}

	public int size() {
		int retValue = 0;
		if (hostList != null)
			retValue = hostList.size();
		return retValue;
	}

	public GraphTreeNode getGraphTreeByHost() {
		return graphTreeByHost;
	}

	public GraphTreeNode getGraphTreeByView() {
		return graphTreeByView;
	}

	/**
	 * Return a grah identified by his hash value
	 * @param id the hash value of the graph
	 * @return the graph found or null of nothing found
	 */
	public RdsGraph getGraphById(int id) {
		return (RdsGraph) graphMap.get(new Integer(id));
	}

	public GraphTreeNode getNodeByPath(String path) {
		GraphTreeNode node = null;
		if(path != null) {
			List pathList = new LinkedList(Arrays.asList(path.split("/")));
			pathList.remove(0);
			String rootName = (String) pathList.get(0);
			if (hostRoot.equals(rootName))
				node = getGraphTreeByHost().getByPath(pathList);
			else if (viewRoot.equals(rootName))
				node = getGraphTreeByView().getByPath(pathList);
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