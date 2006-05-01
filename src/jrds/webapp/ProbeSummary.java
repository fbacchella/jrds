/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
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

import jrds.GraphDesc;
import jrds.HostsList;
import jrds.Period;
import jrds.Probe;

import org.apache.log4j.Logger;
import org.jrobin.core.FetchData;
import org.jrobin.core.RrdException;

/**
 * A servlet wich show the last update values and time
 * @author Fabrice Bacchella
 * @version $Revision: 236 $
 */
public final class ProbeSummary extends HttpServlet {
	static final private Logger logger = Logger.getLogger(ProbeSummary.class);
	
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		res.setContentType("text/plain");
		res.addHeader("Cache-Control", "no-cache");
		ServletOutputStream out = res.getOutputStream();

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
		
		HostsList hl = HostsList.getRootGroup();
		String rrdId = req.getParameter("id");
		Probe probe = hl.getProbeById(Integer.parseInt(rrdId));
		if(probe != null) {
			FetchData fetched = probe.fetchData(begin, end);
			String names[] = fetched.getDsNames();
			for(int i= 0; i< names.length ; i++) {
				String dsName = names[i];
				try {
					out.print(dsName + " ");
					out.print(fetched.getAggregate(dsName, GraphDesc.AVERAGE.toString()) + " ");
					out.print(fetched.getAggregate(dsName, GraphDesc.MIN.toString()) + " ");
					out.println(fetched.getAggregate(dsName, GraphDesc.MAX.toString()));
				} catch (IOException e) {
					logger.error("Probe file " + probe.getRrdName() + "unusable: " + e);
				} catch (RrdException e) {
					logger.error("Error with probe " + probe +": " + e);
				}
			}
		}
		else {
			logger.error("Probe id provied " + rrdId + " invalid");
		}
	}
	
}
