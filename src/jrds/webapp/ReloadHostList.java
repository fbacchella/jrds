/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.webapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.PropertiesManager;

import org.apache.log4j.Logger;

/**
 * This servlet reload the host list file
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ReloadHostList extends HttpServlet {
	/**
	 * 
	 */
	static final private Logger logger = Logger.getLogger(ReloadHostList.class);

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse arg1)
			throws ServletException, IOException {
		ServletContext ctxt = getServletContext();
		PropertiesManager pm = new PropertiesManager();
		InputStream propStream = ctxt.getResourceAsStream("/WEB-INF/jrds.properties");
		if(propStream != null) {
			pm.join(propStream);
		}

		String localPropFile = ctxt.getInitParameter("propertiesFile");
		if(localPropFile != null)
			pm.join(new File(localPropFile));

		pm.update();
		jrds.HostsList.purge();
		jrds.HostsList.getRootGroup().configure(pm);
		logger.info("Configuration dir " + pm.configdir + " rescaned");
		arg1.sendRedirect(req.getContextPath() + "/");
	}
}
