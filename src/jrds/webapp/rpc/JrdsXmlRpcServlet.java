package jrds.webapp.rpc;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.webapp.Configuration;
import jrds.webapp.ParamsBean;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

public class JrdsXmlRpcServlet extends XmlRpcServlet {
	static final private Logger logger = Logger.getLogger(JrdsXmlRpcServlet.class);

	private RequestProcessorFactoryFactory factoryfactory;

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		factoryfactory = new JrdsRequestProcessorFactoryFactory(config);
		super.init(config);
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doPost(HttpServletRequest req,
			HttpServletResponse res) throws IOException, ServletException {
		HostsList hl = getHostsList();

		if(getPropertiesManager().security) {
			ParamsBean p = new ParamsBean();
			p.readAuthorization(req, hl);
			boolean allowed = getPropertiesManager().adminACL.check(p);
			if(! allowed) {
				res.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}

		super.doPost(req, res);
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#getRequestProcessorFactoryFactory()
	 */
	@Override
	public RequestProcessorFactoryFactory getRequestProcessorFactoryFactory() {
		return factoryfactory;
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#log(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void log(String pMessage, Throwable pThrowable) {
		logger.error(pMessage, pThrowable);
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#log(java.lang.String)
	 */
	@Override
	public void log(String pMessage) {
		logger.error(pMessage);
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#newXmlRpcHandlerMapping()
	 */
	@Override
	protected XmlRpcHandlerMapping newXmlRpcHandlerMapping()
	throws XmlRpcException {
		PropertyHandlerMapping map = new PropertyHandlerMapping();
		map.setRequestProcessorFactoryFactory(factoryfactory);
		map.load(getPropertiesManager().extensionClassLoader, Collections.emptyMap());
		map.addHandler(ConfigurationInformations.REMOTENAME, ConfigurationInformations.class);
		return map;
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.webserver.XmlRpcServlet#newXmlRpcServer(javax.servlet.ServletConfig)
	 */
	@Override
	protected XmlRpcServletServer newXmlRpcServer(ServletConfig pConfig)
	throws XmlRpcException {
		XmlRpcServletServer server =  super.newXmlRpcServer(pConfig);
		server.setErrorLogger(new XmlRpcErrorLogger() {
			@Override
			public void log(String pMessage, Throwable pThrowable) {
				logger.error(pMessage, pThrowable);
			}
			@Override
			public void log(String pMessage) {
				logger.error(pMessage);
			}
		});
		return server;
	}

	private Configuration getConfig() {
		ServletContext ctxt = getServletContext();
		return (Configuration) ctxt.getAttribute(Configuration.class.getName());
	}

	private HostsList getHostsList() {
		return getConfig().getHostsList();
	}

	private PropertiesManager getPropertiesManager() {
		return getConfig().getPropertiesManager();
	}

}
