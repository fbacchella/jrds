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
import net.iharder.Base64;

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
                res.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid graph id");
                return;   
            }

            //If the requested end is in the future, the graph should not be cached.
            if(graph.getEnd().after(new Date()) )
                cache = false;

            if(getPropertiesManager().security) {				
                boolean allowed = graph.getACL().check(p);
                logger.trace(jrds.Util.delayedFormatString("Looking if ACL %s allow access to %s", graph.getACL(), this));
                if(! allowed) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid role access");
                    return;
                }
            }

            Date middle = new Date();
            if( ! hl.getRenderer().isReady(graph)) {
                logger.warn("One graph not ready, synchronous rendering");
            }
            res.setContentType("image/png");
            // No caching, the date might be in the future, a period is requested
            // So the image have short lifetime, just one step
            int graphStep = graph.getNode().getProbe().getStep();
            if(p.period.getScale() != 0 || ! cache) {
                res.addDateHeader("Expires", new Date().getTime() + graphStep * 1000);
            }
            res.addDateHeader("Last-Modified", graph.getEnd().getTime());
            res.addHeader("Content-disposition","inline; filename=" + graph.getPngName());
            String eTagBaseString = getServletName() + graph.hashCode();
            res.addHeader("ETag", Base64.encodeBytes(eTagBaseString.getBytes()));
            ServletOutputStream out = res.getOutputStream();
            FileChannel indata = hl.getRenderer().sendInfo(graph);
            //If a cache file exist, try to be smart, but only if caching is allowed
            if(indata != null && cache) {
                logger.debug(jrds.Util.delayedFormatString("graph %s is cached", graph));
                if(indata.size() < Integer.MAX_VALUE)
                    res.setContentLength((int)indata.size());
                WritableByteChannel outC = Channels.newChannel(out);
                indata.transferTo(0, indata.size(), outC);
                indata.close();
            }
            else {
                logger.debug(jrds.Util.delayedFormatString("graph %s not found in cache", graph));
                graph.writePng(out);
            }

            if(logger.isTraceEnabled()) {
                jrds.GraphNode node = hl.getGraphById(p.getId());
                int wh = graph.getDimension().height;
                int rh = graph.getRrdGraph().getRrdGraphInfo().getHeight();
                logger.trace("Delta height:" + (rh - wh) + " for " + node.getGraphDesc());
                Date finish = new Date();
                long duration1 = middle.getTime() - start.getTime();
                long duration2 = finish.getTime() - middle.getTime();
                logger.trace("Graph " + graph + " rendering, started at " + start + ", ran for " + duration1 + ":" + duration2 + "ms");
            }
        } catch (RuntimeException e) {
            if(logger.isDebugEnabled())
                logger.error(e, e);
            else 
                logger.error(e);

            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid graph request");
        }							
    }
}
