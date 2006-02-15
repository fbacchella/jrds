package jrds.webapp;

// ----------------------------------------------------------------------------
// $Id$

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
 * This servlet is used to download the values of a graph as an xml file
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */


public class Download
    extends HttpServlet {
    private static final String CONTENT_TYPE = "text/csv";

    static final private Logger logger = Logger.getLogger(Download.class);

    static final HostsList hl = HostsList.getRootGroup() ;

   public void doGet(HttpServletRequest req, HttpServletResponse res) throws
        ServletException, IOException {
        Date begin = new Date(0);
        Date end = new Date(0);

        calcDate(req.getParameter("begin"), req.getParameter("end"), begin, end);

        res.setContentType(CONTENT_TYPE);

        String rrdId = req.getParameter("id");
        RdsGraph graph = hl.getGraphById(Integer.parseInt(rrdId));
        ServletOutputStream out = res.getOutputStream();

        graph.writeCsv(out, begin, end);
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
