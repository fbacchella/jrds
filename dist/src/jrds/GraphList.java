/*
 * Created on 16 f�vr. 2005
 *
 * TODO 
 */
package jrds;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class GraphList extends HttpServlet {
	static final private Logger logger = JrdsLogger.getLogger(GraphList.class);
	
	static final HostsList hl = HostsList.getRootGroup() ;
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		res.setContentType("text/html; charset=UTF-8");
		java.io.PrintWriter writer = res.getWriter();

		String endString = req.getParameter("end");
		String beginString = req.getParameter("begin");

		writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		writer.println("<html>");
		writer.println("\t<head>");
		writer.println("\t\t<title>Liste des choix</title>");
		writer.println("\t</head>");
		writer.println("\t<body>");
						
		GraphTreeNode node = hl.getNodeByPath(req.getPathInfo());
		if(node != null) {
			Collection allGraphs = node.enumerateChildsGraph();
			for(Iterator i = allGraphs.iterator(); i.hasNext() ;) {
				RdsGraph graph = (RdsGraph) i.next();
				writer.println(Graph.getImgElement(graph, req, beginString, endString));
				
			}
		}
		writer.println("\t</body>");
		writer.println("</html>");
		
	}
}
