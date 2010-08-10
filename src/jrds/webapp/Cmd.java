package jrds.webapp;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.Util;

import org.apache.log4j.Logger;

/**
 * Servlet implementation class Cmd
 */
public class Cmd extends JrdsServlet {
	static final private Logger logger = Logger.getLogger(Cmd.class);
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor. 
	 */
	public Cmd() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		ParamsBean params = new ParamsBean(req, getHostsList(), "command", "arg");

		String command = params.getValue("command");

		logger.debug(Util.delayedFormatString("Command found: %s", command));
		logger.trace(Util.delayedFormatString("path info found: %s", req.getPathInfo()));

		if("reload".equalsIgnoreCase(command)) {
			ServletContext ctxt = getServletContext();
			reload(ctxt);
			res.sendRedirect(req.getContextPath() + "/");
		}
		if("pause".equalsIgnoreCase(command)) {
			ServletContext ctxt = getServletContext();
			pause(ctxt, params.getValue("arg"));
			res.sendRedirect(req.getContextPath() + "/");
		}
	}

	private void reload(final ServletContext ctxt) {
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
		configthread.start();
	}

	private void pause(final ServletContext ctxt, final String arg) {		
		Thread configthread = new Thread("jrds-pause") {
			@Override
			public void run() {
				Configuration config = getConfig();
				try {
					config.getHostsList().lockCollect();
					Thread.sleep(jrds.Util.parseStringNumber(arg, 1) * 1000 );
				} catch (InterruptedException e) {
				}
				config.getHostsList().releaseCollect();
				logger.info("collect restarted");
			}
		};
		configthread.start();
	}

}
