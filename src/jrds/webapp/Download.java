package jrds.webapp;

// ----------------------------------------------------------------------------
// $Id$

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
 * This servlet is used to download the values of a graph as an xml file
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */


public class Download
    extends HttpServlet {
    private static final String CONTENT_TYPE = "text/csv";

    static final HostsList hl = HostsList.getRootGroup() ;

   public void doGet(HttpServletRequest req, HttpServletResponse res) throws
        ServletException, IOException {

	   	Period p = new Period(req.getParameter("begin"), req.getParameter("end"));
		Date begin = p.getBegin();
		Date end = p.getEnd();

        res.setContentType(CONTENT_TYPE);

        String rrdId = req.getParameter("id");
        RdsGraph graph = hl.getGraphById(Integer.parseInt(rrdId));
        ServletOutputStream out = res.getOutputStream();

        graph.writeCsv(out, begin, end);
    }
 }
