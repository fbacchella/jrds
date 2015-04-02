/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.GraphNode;
import jrds.HostsList;
import jrds.Probe;

/**
 * A servlet wich show the last update values and time
 * @author Fabrice Bacchella
 * @version $Revision: 236 $
 */
public final class Last extends JrdsServlet {
	static public String name = null;
	
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		boolean found = false;
		res.setContentType("text/plain");
		res.addHeader("Cache-Control", "no-cache");
		ServletOutputStream out = res.getOutputStream();

		String rrdId = req.getParameter("id");
		HostsList hl = getHostsList();
		GraphNode g = hl.getGraphById(Integer.parseInt(rrdId));
		if(g != null ) {
			Probe<?,?> p = g.getProbe();
			if(p != null) {
				found = true;
				out.println("Last update:" + p.getLastUpdate());
				Map<String, Number> lastValues = p.getLastValues();
				for(Map.Entry<String, Number> e: lastValues.entrySet()) {
					String dsName = e.getKey();
						out.println(dsName + ": " + e.getValue());
				}
			}
			if( ! found) {
				out.println("values not available");
			}
		}
	}
	
}
