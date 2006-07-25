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
import jrds.probe.SumProbe;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse arg1)
			throws ServletException, IOException {
		jrds.HostsList.purge();
		Filter.purge();
		DescFactory.init();
		if(pm.configdir != null)
			DescFactory.scanProbeDir(new File(pm.configdir, "macro"));
		if(pm.configdir != null)
			DescFactory.scanProbeDir(new File(pm.configdir));

		HostsList.getRootGroup().addHost(SumProbe.sumhost);

		logger.info("Configuration dir " + pm.configdir + " rescaned");
		DescFactory.digester = null;
		HostsList.getRootGroup().getMacroList().clear();
		arg1.sendRedirect(req.getContextPath() + "/");
	}
}
