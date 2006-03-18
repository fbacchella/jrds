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
import jrds.Period;
import jrds.RdsGraph;

/**
 * A servlet wich generate a png for a graph
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public final class Graph extends HttpServlet {
	static public String name = null;
	
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		final HostsList hl = HostsList.getRootGroup();

		String scale = req.getParameter("scale");
		Period p = null;
		int scaleVal = -1;
		if(scale != null && (scaleVal = Integer.parseInt(scale)) > 0)
			p = new Period(scaleVal);
		else
			p = new Period(req.getParameter("begin"), req.getParameter("end"));
		Date begin = p.getBegin();
		Date end = p.getEnd();
		
		if("true".equals(req.getParameter("refresh"))) {
			long delta = end.getTime() - begin.getTime();
			end = new Date();
			begin = new Date(end.getTime() - delta);
		}
		
		String rrdId = req.getParameter("id");
		RdsGraph graph = hl.getGraphById(Integer.parseInt(rrdId));
		
		res.setContentType("image/png");
		ServletOutputStream out = res.getOutputStream();
		res.addHeader("Cache-Control", "no-cache");
		graph.writePng(out, begin, end);
	}
	
	public static String getImgUrl(RdsGraph graph, HttpServletRequest req, String begin, String end) {
		StringBuffer retValue = new StringBuffer();
		retValue.append(req.getContextPath());
		retValue.append("/graph?id=" + graph.hashCode());
		if(begin != null)
			retValue.append("&begin=" + begin);
		if(end != null)
			retValue.append("&end=" + end);
		return retValue.toString();
	}
	
	/**
	 * Return an img element, with the URL filled
	 *
	 * @param graph RdsGraph
	 * @param req HttpServletRequest
	 * @param begin String
	 * @param end String
	 * @return String
	 */
	public static String getImgElement(RdsGraph graph, HttpServletRequest req, String begin, String end) {
		StringBuffer retValue = new StringBuffer();
		retValue.append("<img ");
		retValue.append("src='" + req.getContextPath());
		retValue.append("/graph?id=" + graph.hashCode());
		if(begin != null)
			retValue.append("&begin=" + begin);
		if(end != null)
			retValue.append("&end=" + end);
		retValue.append("'");
		retValue.append(" alt='" + graph.getGraphTitle() +"'");
		retValue.append(" border=''");
		int rHeight = graph.getRealHeight();
		if(rHeight > 0)
			retValue.append(" height='" + rHeight + "'");
		int rWidth = graph.getRealWidth();
		if(rWidth > 0)
			retValue.append(" width='" + rWidth + "'");
		retValue.append(">");
		
		return retValue.toString();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	public void init() throws ServletException {
		if(name == null)
			name = getServletName();
		//this.getServletContext().
		super.init();
	}
	
}
