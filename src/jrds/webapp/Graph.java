/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;

import org.apache.log4j.Logger;

/**
 * A servlet wich generate a png for a graph
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public final class Graph extends JrdsServlet {
	static final private Logger logger = Logger.getLogger(Graph.class);
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		try {
			Date start = new Date();
			HostsList hl = getHostsList();

			ParamsBean p = getParamsBean(req);

			jrds.Graph graph = p.getGraph();

			if(getPropertiesManager().security) {				
				boolean allowed = graph.getNode().getACL().check(p);
				if(! allowed) {
					res.setStatus(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}

			Date middle = new Date();
			if( ! hl.getRenderer().isReady(graph)) {
				logger.warn("One graph failed, not ready");
			}
			res.setContentType("image/png");
			ServletOutputStream out = res.getOutputStream();
			res.addHeader("Cache-Control", "no-cache");
			hl.getRenderer().send(graph, out);

			if(logger.isTraceEnabled()) {
				jrds.GraphNode node = hl.getGraphById(p.getId());
				int wh = node.getGraphDesc().getDimension().height;
				int rh = graph.getRrdGraph().getRrdGraphInfo().getHeight();
				logger.trace("Delta height:" + (rh - wh) + " for " + node.getGraphDesc());
				Date finish = new Date();
				long duration1 = middle.getTime() - start.getTime();
				long duration2 = finish.getTime() - middle.getTime();
				logger.trace("Graph " + graph + " rendering, started at " + start + ", ran for " + duration1 + ":" + duration2 + "ms");
			}
		} catch (RuntimeException e) {
			logger.error(e, e);
		}							
	}
}
