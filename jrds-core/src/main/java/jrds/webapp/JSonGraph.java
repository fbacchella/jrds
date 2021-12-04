package jrds.webapp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GraphDesc.Dimension;
import jrds.GraphNode;
import jrds.HostsList;
import jrds.Probe;
import jrds.Renderer;
import jrds.Util;

/**
 * Servlet implementation class JSonGraph
 */
public class JSonGraph extends JSonData {
    static final private Logger logger = LoggerFactory.getLogger(JSonGraph.class);
    private static final long serialVersionUID = 1L;
    private static final List<Integer> periodHistory = Arrays.asList(7, 9, 11, 16);

    @Override
    public boolean generate(JrdsJSONWriter w, HostsList root, ParamsBean params) throws IOException {

        if(params.getPeriod() == null) {
            return false;
        }

        List<GraphNode> graphs = params.getGraphs(this);
        if(params.isSorted() && graphs.size() > 1) {
            Collections.sort(graphs, new Comparator<GraphNode>() {
                public int compare(GraphNode g1, GraphNode g2) {
                    int order = Util.nodeComparator.compare(g1.getName(), g2.getName());
                    if(order == 0)
                        order = Util.nodeComparator.compare(g1.getProbe().getHost().getName(), g2.getProbe().getHost().getName());
                    return order;
                }
            });
        }
        logger.debug("Graphs returned: {}", graphs);
        if(!graphs.isEmpty()) {
            Renderer r = root.getRenderer();
            for(GraphNode gn: graphs) {
                if(!gn.getACL().check(params))
                    continue;
                if(params.isHistory()) {
                    for(int p: periodHistory) {
                        params.setScale(p);
                        doGraph(gn, r, params, w);
                    }
                } else {
                    doGraph(gn, r, params, w);
                }
            }
        }
        return true;
    }

    private void doGraph(GraphNode gn, Renderer r, ParamsBean params, JrdsJSONWriter w) throws IOException {
        jrds.Graph graph = gn.getGraph();
        if (graph == null) {
            return;
        }
        params.configureGraph(graph);

        Map<String, Object> imgProps = new HashMap<String, Object>();
        r.render(graph);
        Probe<?, ?> p = gn.getProbe();
        imgProps.put("probename", p.getName());
        imgProps.put("qualifiedname", graph.getQualifiedName());

        Dimension d = graph.getDimension();
        if(d != null) {
            imgProps.put("height", d.height);
            imgProps.put("width", d.width);
        }
        imgProps.put("graph", params.doArgsMap(graph, true));
        imgProps.put("history", params.doArgsMap(graph, false));
        imgProps.put("probe", params.doArgsMap(p, true));
        imgProps.put("graphnode", params.doArgsMap(gn, true));
        doTree(w, graph.getQualifiedName(), gn.hashCode(), "graph", null, imgProps);
    }

}
