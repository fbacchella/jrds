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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;
import jrds.RdsGraph;

import org.apache.log4j.Logger;

/**
 * A servlet wich generate a png for a graph
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public final class Graph extends HttpServlet {
	static final private Logger logger = Logger.getLogger(Graph.class);
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		try {
			Date start = new Date();
			HostsList hl = HostsList.getRootGroup();

			ParamsBean p = new ParamsBean(req);
			Date begin = p.getBegin();
			Date end = p.getEnd();
			
			if("true".equals(req.getParameter("refresh"))) {
				long delta = end.getTime() - begin.getTime();
				end = new Date();
				begin = new Date(end.getTime() - delta);
			}
			
			RdsGraph graph = hl.getGraphById(p.getId());

			Date middle = new Date();
			if(hl.getRenderer().isReady(graph, begin, end)) {
				res.setContentType("image/png");
				ServletOutputStream out = res.getOutputStream();
				res.addHeader("Cache-Control", "no-cache");
				hl.getRenderer().send(graph, begin, end, out);
			}
			else {
				logger.warn("One graph failed, not ready");
			}

			Date finish = new Date();
			long duration1 = middle.getTime() - start.getTime();
			long duration2 = finish.getTime() - middle.getTime();
			logger.trace("Graph " + graph + " rendering, started at " + start + ", ran for " + duration1 + ":" + duration2 + "ms");
		} catch (RuntimeException e) {
			logger.error(e, e);
		}							
	}
}
