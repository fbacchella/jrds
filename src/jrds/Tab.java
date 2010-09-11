package jrds;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class Tab {
	static private final Logger logger = Logger.getLogger(Tab.class);

	private final Set<String> filters = new TreeSet<String>(jrds.Util.nodeComparator);
	private final Map<String, List<String>> paths = new TreeMap<String, List<String>>(jrds.Util.nodeComparator);
	
	private String name;
	protected HostsList hostlist;

	public Tab(String name) {
		this.name = name;
	}

	public void add(String filter) {
		filters.add(filter);
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
	
	/**
	 * @param hostlist the hostlist to set
	 */
	public void setHostlist(HostsList hostlist) {
		this.hostlist = hostlist;
	}

	public Set<Filter> getFilters() {
		Set<Filter> filtersset = new LinkedHashSet<Filter>(filters.size());
		for(String filtername: filters) {
			Filter f = hostlist.getFilter(filtername);
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
	
	public boolean isFilters() {
		return ! filters.isEmpty();
	}

}
