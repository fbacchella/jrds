package jrds;

// ----------------------------------------------------------------------------
// $Id$

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * <p>Title: </p>
 *
 * <p>Description: This servlet is used to download the values of a graph as an xml file</p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */


public class Download
    extends HttpServlet {
    private static final String CONTENT_TYPE = "text/xml";
    //@todo set DTD
    private static final String DOC_TYPE = null;

    static final private Logger logger = JrdsLogger.getLogger(Graph.class);

    static final HostsList hl = HostsList.getRootGroup() ;

   public void doGet(HttpServletRequest req, HttpServletResponse res) throws
        ServletException, IOException {
        Date begin = new Date(0);
        Date end = new Date(0);

        calcDate(req.getParameter("begin"), req.getParameter("end"), begin, end);

        res.setContentType(CONTENT_TYPE);
        //PrintWriter out = response.getWriter();
        //out.println("<?xml version=\"1.0\"?>");
        //if (DOC_TYPE != null) {
        //    out.println(DOC_TYPE);
        //}

        String rrdId = req.getParameter("id");
        RdsGraph graph = hl.getGraphById(Integer.parseInt(rrdId));
        ServletOutputStream out = res.getOutputStream();

        graph.writeXml(out, begin, end);
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
        long lbegin = 0 - Calendar.DATE;
        long lend = -1;

        try {
            if(sbegin != null)
                lbegin = Long.parseLong(sbegin);
        }
        catch (NumberFormatException ex) {}

        try {
            if(send != null)
                lend = Long.parseLong(send);
        }
        catch (NumberFormatException ex) {}

        if(lend == -1)
            end.setTime(System.currentTimeMillis());
        else
            end.setTime(lend);

        if(lbegin < 0) {
            Calendar cbegin = new GregorianCalendar();
            cbegin.setTime(end);
            cbegin.add((int)(0 - lbegin), -1);
            begin.setTime(cbegin.getTimeInMillis());
        }
        else
            begin.setTime(lbegin);
    }
}
