package jrds.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import jrds.Filter;
import jrds.FilterTag;
import jrds.FilterXml;
import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostsList;

import org.apache.log4j.Logger;

/**
 * Servlet implementation class JSonTree
 */
public class JSonTree extends JSonData {
	static final private Logger logger = Logger.getLogger(JSonTree.class);

	private static final long serialVersionUID = 1L;

	@Override
	public boolean generate(ServletOutputStream out, HostsList root,
			ParamsBean params) throws IOException {
		Filter f = params.getFilter();
		if( f != null) {
			return evaluateFilter(params, out, root, f);
		}
		else 
			return dumpRoots(out, root);
	}

	boolean evaluateFilter(ParamsBean params, ServletOutputStream out, HostsList root, Filter f) throws IOException {
		logger.debug("Dumping with filter" + f);
		Collection<GraphTree> level = root.getGraphsRoot();
		logger.trace("This level size: " + level.size());

		//A first pass to see if there is only one root
		//Jump into the childs it's the case
		//The tree is parsed twice, that's not optimal
		if( (f  instanceof FilterXml)) {
			int count =0;
			for(GraphTree tree: root.getGraphsRoot()) {
				if(tree.enumerateChildsGraph(f).size() > 0) {
					count++;
					level = tree.getChildsMap().values();
				}
			}
			if(count > 1)
				level = root.getGraphsRoot();
		}

		for(GraphTree tree: level) {
			tree = f.setRoot(tree);
			if(tree != null)
				sub(params, out, tree, "tree", f, "", tree.hashCode());
		}
		return true;
	}

	boolean dumpRoots(ServletOutputStream out, HostsList root) throws IOException {
		List<String> tagsref = new ArrayList<String>();
		for(String filterName: root.getAllFiltersNames()) {
			Filter filter = root.getFilter(filterName);
			String type = "filter";
			if(filter instanceof FilterTag){
				logger.trace("Found filter tag: " + filter);
				tagsref.add(Integer.toString(filter.hashCode()));
				type="subfilter";
			}
			Map<String, String> href = new HashMap<String, String>();
			href.put("filter", filterName);
			out.print(doNode(filterName, filter.hashCode(), type, null, href));
		}
		if(tagsref.size() > 0) {
			out.print(doNode("All tags", tagsref.hashCode(), "filter", tagsref));
		}
		return true;
	}

	String sub(ParamsBean params, ServletOutputStream out, GraphTree gt, String type, Filter f, String path, int base) throws IOException {
		String id = null;
		String subpath = path + "/" + gt.getName();
		logger.trace(subpath);
		boolean hasChild = false;
		Map<String, GraphTree> childs = gt.getChildsMap();

		List<String> childsref = new ArrayList<String>();
		for(Map.Entry<String, GraphTree>e: childs.entrySet()) {
			String childid = sub(params, out, e.getValue(), "node", f, subpath, base);
			if(childid != null) {
				hasChild = true;
				childsref.add(childid);
			}
		}

		for(Map.Entry<String, GraphNode> leaf: gt.getGraphsSet().entrySet()) {
			GraphNode child = leaf.getValue();
			String leafName = leaf.getKey();
			if(f.acceptGraph(child, gt.getPath() + "/" + child.getName())) {
				hasChild = true;
				String graphid = base + "." + child.hashCode();
				childsref.add(graphid );
				out.print(doNode(leafName, graphid, "graph", null));
			}
		}

		if(hasChild) {
			id = base + "." +  gt.getPath().hashCode();
			out.print(doNode(gt.getName(), id, type, childsref));
		}
		return id;
	}

}
