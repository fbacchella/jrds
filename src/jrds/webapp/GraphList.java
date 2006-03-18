/*
 * Created on 16 févr. 2005
 *
 * TODO
 */
package jrds.webapp;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import jrds.GraphTree;
import jrds.HostsList;
import jrds.Period;
import jrds.RdsGraph;


/**
 * Used to generate a tree of all graphs
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphList extends HttpServlet {

	static final private Logger logger = Logger.getLogger(GraphList.class);

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setContentType("text/html; charset=UTF-8");
		java.io.PrintWriter writer = res.getWriter();
		
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
		
		writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		writer.println("<html>");
		writer.println("\t<head>");
		writer.println("\t\t<meta http-equiv='content-type' content='text/html;charset=utf-8'>");
		writer.println("\t\t<title>Liste des choix</title>");
		writer.println("\t</head>");
		writer.println("\t<body>");
		
		GraphTree node = HostsList.getRootGroup().getNodeByPath(req.getPathInfo());
		if(node != null) {
			for(Iterator i = node.enumerateChildsGraph().iterator(); i.hasNext() ;) {
				StringBuffer imgElement = new StringBuffer();
				
				imgElement.append("<img ");
				//We build the Url
				RdsGraph graph = (RdsGraph) i.next();
				StringBuffer urlBuffer = new StringBuffer();
				urlBuffer.append(req.getContextPath());
				urlBuffer.append("/graph?id=" + graph.hashCode());
				if(scale != null)
					urlBuffer.append("&scale=" + scale);
				else {
					if(begin != null)
						urlBuffer.append("&begin=" + begin);
					if(end != null)
						urlBuffer.append("&end=" + end);
				}
				imgElement.append("src='" + urlBuffer + "' ");
				
				//A few more attributes
				imgElement.append("alt='" + graph.getQualifieName() + "' ");
				int rHeight = graph.getRealHeight();
				if(rHeight > 0)
					imgElement.append("height='" + Integer.toString(rHeight) + "' ");
				int rWidth = graph.getRealWidth();
				if(rWidth > 0)
					imgElement.append("width='" + Integer.toString(rWidth)+ "' ");
				
				imgElement.append(" />");
				writer.println(imgElement);
			}
		}
		else {
			logger.warn("No graphs found under " + req.getPathInfo());
		}
		writer.println("\t</body>");
		writer.println("</html>");
	}
}
