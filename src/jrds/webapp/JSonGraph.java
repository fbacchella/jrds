package jrds.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.apache.log4j.Logger;

import jrds.Filter;
import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.Probe;
import jrds.Renderer;

/**
 * Servlet implementation class JSonGraph
 */
public class JSonGraph extends JSonData {
	static final private Logger logger = Logger.getLogger(JSonGraph.class);
	private static final long serialVersionUID = 1L;
	private int periodHistory[] = {7, 9, 11, 16};

	@Override
	public boolean generate(ServletOutputStream out, HostsList root,
			ParamsBean params) throws IOException {

		Filter filter = params.getFilter();
		int id = params.getId();

		if(params.getPeriod() == null) {
			return false;
		}

		GraphTree node = root.getNodeById(id);
		List<GraphNode> graphs = new ArrayList<GraphNode>();
		if(node != null) {
			logger.debug("Tree found: " + node);
			for(GraphNode graph: node.enumerateChildsGraph(filter)) {
				graphs.add(graph);
			}
			if(params.isSorted()) {
				Collections.sort(graphs, new Comparator<GraphNode>() {
					public int compare(GraphNode g1, GraphNode g2) {
						int order = String.CASE_INSENSITIVE_ORDER.compare(g1.getName(), g2.getName());
						if(order == 0)
							order = String.CASE_INSENSITIVE_ORDER.compare(g1.getProbe().getHost().getName(), g2.getProbe().getHost().getName());
						return order;
					}
				});
			}
		}
		else {
			GraphNode gn = root.getGraphById(id);
			if(gn != null)
				graphs.add(gn);
		}
		logger.debug("Graphs found:" +  graphs);
		if( ! graphs.isEmpty()) {
			Renderer r = root.getRenderer();
			for(GraphNode gn: graphs) {
				if(params.isHistory()) {
					for(int p: periodHistory) {
						params.setScale(p);
						doGraph(gn, r, params, out);
					}
				}
				else {
					doGraph(gn, r, params, out);
				}
			}
		}
		return true;
	}

	private void doGraph(GraphNode gn, Renderer r, ParamsBean params, ServletOutputStream out) throws IOException {
		jrds.Graph graph = gn.getGraph();
		params.configureGraph(graph);

		Map<String, String> imgProps = new HashMap<String, String>();
		r.render(graph);
		Probe<?,?> p = gn.getProbe();
		imgProps.put("probename", p.getName());
		imgProps.put("qualifiedname", graph.getQualifieName());

		imgProps.put("popuparg", params.makeObjectUrl("popup.html", graph, true));
		imgProps.put("detailsarg", params.makeObjectUrl("details", gn, true));
		imgProps.put("historyarg", params.makeObjectUrl("history.html", gn, false));
		imgProps.put("savearg", params.makeObjectUrl("download", gn, true));
		imgProps.put("imghref", params.makeObjectUrl("graph",graph, true));
		imgProps.put("qualifiedname", graph.getQualifieName());

		out.print(doNode(graph.getQualifieName(), gn.hashCode(), "graph", null, imgProps));

	}

}
