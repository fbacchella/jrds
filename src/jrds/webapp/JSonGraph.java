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

	@Override
	public void generate(ServletOutputStream out, HostsList root,
			ParamsBean params) throws IOException {

		Filter filter = params.getFilter();
		int id = params.getId();

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
			GraphNode graph = root.getGraphById(id);
			if(graph != null)
				graphs.add(graph);
		}

		if( ! graphs.isEmpty()) {
			Renderer r = root.getRenderer();
			for(GraphNode gn: graphs) {
				Map<String, String> imgProps = new HashMap<String, String>();
				jrds.Graph graph = gn.getGraph();
				params.configureGraph(graph);
				r.render(graph);
				
				Probe p = gn.getProbe();
				imgProps.put("probename", p.getName());

				imgProps.put("popuparg", params.makeObjectUrl("popup.jsp", graph, true));
				imgProps.put("detailsarg", params.makeObjectUrl("details", graph, true));
				imgProps.put("historyarg", params.makeObjectUrl("history.jsp", graph, false));
				imgProps.put("savearg", params.makeObjectUrl("download", graph, true));
				imgProps.put("imghref", params.makeObjectUrl("graph",graph, true));

				out.print(doNode(graph.getQualifieName(), graph.hashCode(), "graph", null, imgProps));
			}
		}

	}

}
