/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jrds.*;

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
		final HostsList hl = HostsList.getRootGroup() ;
		Date begin = new Date(0);
		Date end = new Date(0);
		
		calcDate(req.getParameter("begin"), req.getParameter("end"), begin, end);
		
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
	
	/**
	 * @param graph
	 * @param req
	 * @param begin
	 * @param end
	 * @return A String containing the absolute URL of the image
	 */
	public static String getImgUrl(RdsGraph graph, HttpServletRequest req, long begin, long end) {
		StringBuffer retValue = new StringBuffer();
		retValue.append(req.getContextPath());
		retValue.append("/graph?id=" + graph.hashCode());
		retValue.append("&begin=" + begin);
		retValue.append("&end=" + end);
		return retValue.toString();
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
	
	/**
	 * Calculate date from string parametrs comming from the URL
	 *
	 * @param sbegin String
	 * @param send String
	 * @param begin The calculated begin date
	 * @param end The calculated end date
	 */
	static private void calcDate(String sbegin, String send, Date begin, Date end){
		long lbegin = Calendar.DATE * -1;
		long lend = -1;

		try {
			if(sbegin != null)
				lbegin = Long.parseLong(sbegin);
		}
		catch (NumberFormatException ex) {}
		
		try {
			if("NOW".compareToIgnoreCase(send) == 0)
				lend = -1;
			else if(send != null)
				lend = Long.parseLong(send);
		}
		catch (NumberFormatException ex) {}
		
		if(lend == -1)
			end.setTime(System.currentTimeMillis());
		else
			end.setTime(lend);
		
		if(lbegin <= 0) {
			Calendar cbegin = new GregorianCalendar();
			cbegin.setTime(end);
			cbegin.add(convertScale(lbegin), -1);
			begin.setTime(cbegin.getTimeInMillis());
		}
		else
			begin.setTime(lbegin);
	}
	
	/**
	 * Convert a int to a calendar field
	 * @param scale
	 * @return
	 */
	static private int convertScale(long scale) {
		//Failsafe, the whole year
		int retValue = Calendar.YEAR;
		if(scale == 0)
			retValue = Calendar.DAY_OF_MONTH;
		else if(scale == -1)
			retValue = Calendar.WEEK_OF_YEAR;
		else if(scale == -2)
			retValue = Calendar.MONTH;
		else if(scale == -3)
			retValue = Calendar.YEAR;
		return retValue;
	}
	
}
