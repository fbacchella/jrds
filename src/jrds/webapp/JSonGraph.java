package jrds.webapp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.Filter;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.GraphTree;
import jrds.HostsList;
import jrds.Probe;
import jrds.Renderer;

import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * Servlet implementation class JSonGraph
 */
public class JSonGraph extends JSonData {
	static final private Logger logger = Logger.getLogger(JSonGraph.class);
	private static final long serialVersionUID = 1L;
	private int periodHistory[] = {7, 9, 11, 16};

	@Override
	public boolean generate(JrdsJSONWriter w, HostsList root,
			ParamsBean params) throws IOException, JSONException {

		if(params.getPeriod() == null) {
			return false;
		}

		List<GraphNode> graphs = getGraphs(root, params);
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
		logger.debug("Graphs found:" +  graphs);
		if( ! graphs.isEmpty()) {
			Renderer r = root.getRenderer();
			for(GraphNode gn: graphs) {
				if(params.isHistory()) {
					for(int p: periodHistory) {
						params.setScale(p);
						doGraph(gn, r, params, w);
					}
				}
				else {
					doGraph(gn, r, params, w);
				}
			}
		}
		return true;
	}

	private List<GraphNode> getGraphs(HostsList root, ParamsBean params) {
		int id = params.getId();
		String dsName = params.getDsName();

		GraphTree node = root.getNodeById(id);
		if(node != null) {
			logger.debug("Tree found: " + node);
			Filter filter = params.getFilter();
			if(filter.getACL().check(params))
				return node.enumerateChildsGraph(filter);
		}
		else if(params.getPid() != 0 && dsName != null) {
			if(! allowed(params, root.getDefaultRoles()))
				return Collections.emptyList();
			Probe<?, ?> p = params.getProbe();
			logger.debug("Probe found: " + p);
			if(p == null) {
				logger.error("Looking for unknown probe");
				return Collections.emptyList();
			}

			Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
			String graphDescName = p.getName() + "." + dsName;

			GraphDesc gd = new GraphDesc();
			gd.setName(graphDescName);
			gd.setGraphName(p.getHost().getName() + "." + p.getName() + "." + dsName);
			gd.setGraphTitle(p.getName() + "." + dsName + " on ${host}");
			gd.add(dsName, GraphDesc.LINE);
			gd.initializeLimits(g2d);

			GraphNode gn = new GraphNode(p, gd);
			return Collections.singletonList(gn);
		}
		else {
			GraphNode gn = root.getGraphById(id);
			if(gn != null && gn.getACL().check(params))
				return Collections.singletonList(gn);
		}

		return Collections.emptyList();
	}

	private void doGraph(GraphNode gn, Renderer r, ParamsBean params, JrdsJSONWriter w) throws IOException, JSONException {
		jrds.Graph graph = gn.getGraph();
		params.configureGraph(graph);

		Map<String, String> imgProps = new HashMap<String, String>();
		r.render(graph);
		Probe<?,?> p = gn.getProbe();
		imgProps.put("probename", p.getName());
		imgProps.put("qualifiedname", graph.getQualifieName());

		imgProps.put("popuparg", params.makeObjectUrl("popup.html", graph, true));
		imgProps.put("detailsarg", params.makeObjectUrl("details", p, true));
		imgProps.put("historyarg", params.makeObjectUrl("history.html", gn, false));
		imgProps.put("savearg", params.makeObjectUrl("download", gn, true));
		imgProps.put("imghref", params.makeObjectUrl("graph",graph, true));
		imgProps.put("qualifiedname", graph.getQualifieName());
		GraphDesc gd = gn.getGraphDesc();
		if(gd !=null && gd.getDimension() != null) {
			imgProps.put("height", Integer.toString(gd.getDimension().height));
			imgProps.put("width", Integer.toString(gd.getDimension().width));
		}

		doNode(w, graph.getQualifieName(), gn.hashCode(), "graph", null, imgProps);
	}

}
