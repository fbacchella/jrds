/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.webapp;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jrds.*;

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
	private static final PropertiesManager pm = PropertiesManager.getInstance();

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {
		HostsList.fill(new File(pm.configfilepath));
		logger.info("Configuration file " + pm.configfilepath + " reloaded");
	}
}
