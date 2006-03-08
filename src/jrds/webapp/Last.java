/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;
import jrds.Probe;
import jrds.RdsGraph;

/**
 * A servlet wich show the last update values and time
 * @author Fabrice Bacchella
 * @version $Revision: 236 $
 */
public final class Last extends HttpServlet {
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
		HostsList hl = HostsList.getRootGroup();
		RdsGraph g = hl.getGraphById(Integer.parseInt(rrdId));
		if(g != null ) {
			Probe p = g.getProbe();
			if(p != null) {
				found = true;
				Map lastValues = p.getLastValues();
				for(Iterator i = lastValues.entrySet().iterator(); i.hasNext(); ) {
					Map.Entry e = (Map.Entry) i.next();
					String dsName = (String) e.getKey();
					if("Last update".equals(dsName)) {
						Date lastUpdate = (Date) e.getValue();
						out.println("Last update:" + lastUpdate);
					}
					else {
						Double value = (Double) e.getValue();
						out.println(dsName + ": " + value);
					}
				}
			}
			if( ! found) {
				out.println("values not available");
			}
		}
	}
	
}
