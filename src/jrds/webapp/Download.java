package jrds.webapp;

//----------------------------------------------------------------------------
//$Id$

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * This servlet is used to download the values of a graph as an xml file
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */

public class Download extends JrdsServlet {
    static final private Logger logger = Logger.getLogger(Download.class);
	private static final String CONTENT_TYPE = "text/csv";

	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {

		ParamsBean params = getParamsBean(req);

		res.setContentType(CONTENT_TYPE);

		jrds.Graph graph = params.getGraph(this);
        if(graph == null) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;   
        }
        
        if(getPropertiesManager().security) {               
            boolean allowed = graph.getACL().check(params);
            logger.trace(jrds.Util.delayedFormatString("Looking if ACL %s allow access to %s", graph.getACL(), this));
            if(! allowed) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

		ServletOutputStream out = res.getOutputStream();
		res.addHeader("content-disposition","attachment; filename="+ graph.getPngName().replaceFirst("\\.png",".csv"));

		graph.writeCsv(out);
	}
}
