package jrds.webapp;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import jrds.Filter;
import jrds.FilterTag;
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
	public void generate(ServletOutputStream out, HostsList root,
			ParamsBean params) throws IOException {
		Filter f = params.getFilter();
		if( f != null) {
			evaluateFilter(params, out, root, f);
		}
		else 
			dumpRoots(out, root);
	}
	
	void evaluateFilter(ParamsBean params, ServletOutputStream out, HostsList root, Filter f) throws IOException {
		for(GraphTree tree: root.getGraphsRoot()) {
			tree = f.setRoot(tree);
			sub(params, out, tree, "tree", f, "", tree.hashCode());
		}
	}

	void dumpRoots(ServletOutputStream out, HostsList root) throws IOException {
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
			href.put("href", "draft.html?filter=" +URLEncoder.encode(filterName, "UTF-8") );
			out.print(doNode(filterName, filter.hashCode(), type, null, href));
		}
		if(tagsref.size() > 0) {
			out.print(doNode("All tags", tagsref.hashCode(), "filter", tagsref));
		}
	}

	String sub(ParamsBean params, ServletOutputStream out, GraphTree gt, String type, Filter f, String path, int base) throws IOException {
		String id = null;
		String subpath = path + "/" + gt.getName();
		logger.trace(subpath);
		boolean hasChild = false;
		Map<String, GraphTree> childs = gt.getChildsMap();

		//    ['http://ng171:15610/index/taya-infkw/0/r1/GetProbeValues','/jrds/index.jsp?filter=All+hosts&scale=7&id=-1156832049&refresh=true']
		
		List<String> childsref = new ArrayList<String>();
		for(Map.Entry<String, GraphTree>e: childs.entrySet()) {
			String childid = sub(params, out, e.getValue(), "node", f, subpath, base);
			if(childid != null) {
				hasChild = true;
				childsref.add(childid);
			}
		}

		for(GraphNode child: gt.getGraphsSet().values()) {
			if(f.acceptGraph(child, subpath + "/" + child.getName())) {
				hasChild = true;
				String graphid = base + "." + child.hashCode();
				childsref.add(graphid );
				out.print(doNode(child.getName(), graphid, "graph", null));
			}
		}

		if(hasChild) {
			id = base + "." +  gt.getPath().hashCode();
			out.print(doNode(gt.getName(), id, type, childsref));
		}
		return id;
	}

}
