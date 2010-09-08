package jrds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class Tab {
	static private final Logger logger = Logger.getLogger(Tab.class);

	private final List<String> filters = new ArrayList<String>();
	private final Map<String, List<String>> paths = new HashMap<String, List<String>>();
	
	private String name;
	private HostsList hostlist;

	public Tab(String name) {
		this.name = name;
	}

	public void add(String filter) {
		filters.add(filter);
	}

	public void add(String id, List<String> path) {
		paths.put(id, path);
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#getGraphList()
	 */
	public Collection<GraphNode> getGraphList() {
		logger.debug(jrds.Util.delayedFormatString("Paths to add in %s: %s", name, paths));
		Set<GraphNode> graphs = new HashSet<GraphNode>();
		for(Map.Entry<String , List<String>> e: paths.entrySet()) {
			String id = e.getKey();
			List<String> path = e.getValue();
			GraphNode sourcegn = getHostList().getGraphById(id.hashCode());
			GraphDesc sourcegd = sourcegn.getGraphDesc();

			GraphDesc newgd;
			try {
				newgd = (GraphDesc) sourcegd.clone();
				newgd.setHostTree(path);
				GraphNode newgn = new GraphNode(sourcegn.getProbe(), newgd);
				graphs.add(newgn);
			} catch (CloneNotSupportedException ex) {
			}
		}
		return graphs;
	}
	
	public GraphTree getGraphTree() {
		GraphTree gt = GraphTree.makeGraph(name); 
		for(Map.Entry<String , List<String>> e: paths.entrySet()) {
			String id = e.getKey();
			List<String> path = e.getValue();
			GraphNode gn = getHostList().getGraphById(id.hashCode());
			if(gn == null) {
				logger.warn(jrds.Util.delayedFormatString("Graph not found for %s: %s", name, id));
				continue;
			}
			gt.addGraphByPath(path, gn);
		}
		return gt;
	}
	
	/**
	 * @param hostlist the hostlist to set
	 */
	public void setHostlist(HostsList hostlist) {
		this.hostlist = hostlist;
	}

	protected HostsList getHostList() {
		return hostlist;
	}

	public Set<Filter> getFilters() {
		Set<Filter> filtersset = new HashSet<Filter>(filters.size());
		for(String filtername: filters) {
			Filter f = getHostList().getFilter(filtername);
			if(f != null)
				filtersset.add(f);
		}
		return filtersset;
	}

	/* (non-Javadoc)
	 * @see jrds.Probe#toString()
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

}
