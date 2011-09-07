/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
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

			ParamsBean p = new ParamsBean(req, hl, "host", "graphname");
			
			//Let have a little cache control
			boolean cache = true;
			String cachecontrol = req.getHeader("Cache-Control");
			if(cachecontrol != null && "no-cache".equals(cachecontrol.toLowerCase().trim()))
			    cache = false;

			jrds.Graph graph = p.getGraph(this);
			
			if(graph == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;   
			}

			if(getPropertiesManager().security) {				
				boolean allowed = graph.getACL().check(p);
				logger.trace(jrds.Util.delayedFormatString("Looking if ACL %s allow access to %s", graph.getACL(), this));
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
			if(p.period.getScale() != 0) {
			    res.addDateHeader("Expires", graph.getEnd().getTime() + getPropertiesManager().step * 1000);
			}
			res.addDateHeader("Last-Modified", graph.getEnd().getTime());
			res.addHeader("content-disposition","attachment; filename=" + graph.getPngName());
			res.addHeader("ETag", jrds.Base64.encodeString(getServletName() + graph.hashCode()));
			FileChannel indata = hl.getRenderer().sendInfo(graph);
			//If a cache file exist, try to be smart, but only if caching is allowed
			if(indata != null && cache) {
			    res.addIntHeader("Content-Length", (int)indata.size());
                WritableByteChannel outC = Channels.newChannel(out);
                indata.transferTo(0, indata.size(), outC);
                indata.close();
			}
			else {
			    graph.writePng(out);
			}

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
