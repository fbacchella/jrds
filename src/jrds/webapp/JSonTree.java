package jrds.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jrds.Filter;
import jrds.FilterTag;
import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostsList;

import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * Servlet implementation class JSonTree
 */
public class JSonTree extends JSonData {
	static final private Logger logger = Logger.getLogger(JSonTree.class);

	@Override
	public boolean generate(JrdsJSONWriter w, HostsList root, ParamsBean params) throws IOException, JSONException {
		Filter f = params.getFilter();
		if( f != null) {
			return evaluateFilter(params, w, root, f);
		}
		else {
			String filterName = params.getValue("filter");
			if(filterName != null && "All tags".equals(filterName.trim())) {
				return dumpTags(w, root);
			}
			else
				return dumpRoots(w, root);
		}
	}

	boolean evaluateFilter(ParamsBean params, JrdsJSONWriter w, HostsList root, Filter f) throws IOException, JSONException {
		logger.debug("Dumping with filter" + f);
		Collection<GraphTree> level = root.getGraphsRoot();
		logger.trace("Graphs root: " + level);

		//We construct the graph tree root to use
		//The tree is parsed twice, that's not optimal
		Collection<GraphTree> rootToDo = new HashSet<GraphTree>(level.size());
		for(GraphTree tree: root.getGraphsRoot()) {
			GraphTree testTree = f.setRoot(tree);
			if(testTree != null && ! rootToDo.contains(testTree) && testTree.enumerateChildsGraph(f).size() > 0) {
				rootToDo.add(testTree);
			}
		}

		//Look for the first level with many childs
		while(rootToDo.size() == 1) {
			GraphTree child = rootToDo.iterator().next();
			rootToDo = child.getChildsMap().values();
		}

		for(GraphTree tree: rootToDo) {
			sub(params, w, tree, "tree", f, "", tree.hashCode());
		}
		return true;
	}

	boolean dumpTags(JrdsJSONWriter w, HostsList root) throws IOException, JSONException {
		List<String> tagsref = new ArrayList<String>();
		for(String filterName: root.getAllFiltersNames()) {
			Filter filter = root.getFilter(filterName);
			String type = "filter";
			if(filter instanceof FilterTag){
				logger.trace("Found filter tag: " + filter);
				tagsref.add(Integer.toString(filter.hashCode()));
				type="subfilter";
				Map<String, String> href = new HashMap<String, String>();
				href.put("filter", filterName);
				doNode(w,filterName, filter.hashCode(), type, null, href);
			}
		}
		if(tagsref.size() > 0) {
			doNode(w,"All tags", tagsref.hashCode(), "filter", tagsref);
		}
		return true;
	}

	boolean dumpRoots(JrdsJSONWriter w, HostsList root) throws IOException, JSONException {
		for(String filterName: root.getAllFiltersNames()) {
			Filter filter = root.getFilter(filterName);
			String type = "filter";
			if(!(filter instanceof FilterTag)){
				Map<String, String> href = new HashMap<String, String>();
				href.put("filter", filterName);
				doNode(w,filterName, filter.hashCode(), type, null, href);
			}
		}
		return true;
	}

	String sub(ParamsBean params, JrdsJSONWriter w, GraphTree gt, String type, Filter f, String path, int base) throws IOException, JSONException {
		String id = null;
		String subpath = path + "/" + gt.getName();
		logger.trace(subpath);
		boolean hasChild = false;
		Map<String, GraphTree> childs = gt.getChildsMap();

		List<String> childsref = new ArrayList<String>();
		for(Map.Entry<String, GraphTree>e: childs.entrySet()) {
			String childid = sub(params, w, e.getValue(), "node", f, subpath, base);
			if(childid != null) {
				hasChild = true;
				childsref.add(childid);
			}
		}

		for(Map.Entry<String, GraphNode> leaf: gt.getGraphsSet().entrySet()) {
			GraphNode child = leaf.getValue();
			if(getPropertiesManager().security &&  ! child.getACL().check(params))
				continue;
			String leafName = leaf.getKey();
			if(f.acceptGraph(child, gt.getPath() + "/" + child.getName())) {
				hasChild = true;
				String graphid = base + "." + child.hashCode();
				childsref.add(graphid );
				doNode(w,leafName, graphid, "graph", null);
			}
		}

		if(hasChild) {
			id = base + "." +  gt.getPath().hashCode();
			doNode(w,gt.getName(), id, type, childsref);
		}
		return id;
	}

}
