package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * This servlet reload the host list file
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class ReloadHostList extends JrdsServlet {
	static final private Logger logger = Logger.getLogger(ReloadHostList.class);

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		final ServletContext ctxt = getServletContext();
		
		ParamsBean params = new ParamsBean(req, getHostsList());
		if(! allowed(params, getPropertiesManager().adminACL, req, res))
			return;

		Thread configthread = new Thread("jrds-new-config") {
			@Override
			public void run() {
				Configuration oldConfig = getConfig();
				Configuration newConfig = new Configuration(ctxt);
				oldConfig.stop();
				newConfig.start();
				ctxt.setAttribute(Configuration.class.getName(), newConfig);
				logger.info("Configuration rescaned");
			}
		};
		if(params.getValue("sync") != null) {
			configthread.run();
			return;
		}

		configthread.start();
		res.sendRedirect(req.getContextPath() + "/");
	}
}
