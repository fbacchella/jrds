/*##########################################################################
 _##
 _##  $Id: Graph.java 360 2006-08-23 09:31:58 +0000 (mer., 23 août 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;
import jrds.RdsHost;

/**
 * A few stats for jrds inner status
 * @author Fabrice Bacchella
 * @version $Revision: 360 $
 */
public class Status extends JrdsServlet {
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		
		HostsList hl = getHostsList();

		ParamsBean params = new ParamsBean();
		params.parseReq(req, hl);
		if(! allowed(params, getPropertiesManager().defaultRoles)) {
			res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		res.setContentType("text/plain");
		res.addHeader("Cache-Control", "no-cache");

		Collection<RdsHost> hosts = hl.getHosts();
		int numHosts = hosts.size();
		int numProbes = 0;
		for(RdsHost h: hosts) {
			numProbes += h.getProbes().size();
		}
		PrintWriter writer = res.getWriter();
		writer.println("Hosts: " + numHosts);
		writer.println("Probes: " + numProbes);
		HostsList.Stats stats = hl.getStats();
		Date lastCollect;
		long runtime;
		synchronized(stats) {
			lastCollect = stats.lastCollect;
			runtime = stats.runtime;
		}
		long lastCollectAgo = (System.currentTimeMillis() - lastCollect.getTime())/1000;
		writer.println("Last collect: " + lastCollectAgo  + "s ago (" + lastCollect + ")" );
		writer.println("Last running duration: " + runtime / 1000 + "s");
		writer.flush();
	}
}
