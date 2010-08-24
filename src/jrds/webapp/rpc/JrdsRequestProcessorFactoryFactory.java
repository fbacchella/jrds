package jrds.webapp.rpc;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import jrds.webapp.Configuration;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;

public class JrdsRequestProcessorFactoryFactory extends RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory {

	public interface InitializableRequestProcessor {
		void init(Configuration config) throws XmlRpcException;
	}

	private final ServletConfig config;

	public JrdsRequestProcessorFactoryFactory(ServletConfig config) {
		super();
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory#getRequestProcessor(java.lang.Class, org.apache.xmlrpc.XmlRpcRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest)
	throws XmlRpcException {
		InitializableRequestProcessor proc = (InitializableRequestProcessor) super.getRequestProcessor(pClass, pRequest);
		proc.init(getConfig());
		return proc;
	}

	protected Configuration getConfig() {
		ServletContext ctxt = config.getServletContext();
		return (Configuration) ctxt.getAttribute(Configuration.class.getName());
	}

}
