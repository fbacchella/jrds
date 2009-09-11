package jrds.webapp;

//----------------------------------------------------------------------------
//$Id$

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;

/**
 * This servlet is used to download the values of a graph as an xml file
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */

public class Download extends HttpServlet {
	private static final String CONTENT_TYPE = "text/csv";

	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		ParamsBean params = new ParamsBean(req, (HostsList) getServletContext().getAttribute(HostsList.class.getName()));

		res.setContentType(CONTENT_TYPE);

		jrds.Graph graph = params.getGraph();
		ServletOutputStream out = res.getOutputStream();
		res.addHeader("content-disposition","attachment; filename="+ graph.getPngName().replaceFirst("\\.png",".csv"));

		graph.writeCsv(out);
	}
}
