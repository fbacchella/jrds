package jrds.webapp;

import java.util.Collections;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import jrds.HostsList;
import jrds.PropertiesManager;

public abstract class JrdsServlet extends HttpServlet {
	static final private Logger logger = Logger.getLogger(JrdsServlet.class);

	protected Configuration getConfig() {
		ServletContext ctxt = getServletContext();
		return (Configuration) ctxt.getAttribute(Configuration.class.getName());
	}
	
	protected HostsList getHostsList() {
		return getConfig().getHostsList();
	}

	protected PropertiesManager getPropertiesManager() {
		return getConfig().getPropertiesManager();
	}

	protected ParamsBean getParamsBean(HttpServletRequest request) {
		return new ParamsBean(request, getHostsList());
	}
	
	protected boolean allowed(ParamsBean params, Set<String> roles) {
		if(getPropertiesManager().security) {
			if(roles.contains("ANONYMOUS"))
				return true;
			if(logger.isTraceEnabled()) {
				logger.trace("Checking if roles " + params.getRoles() + " in roles " + roles);
				logger.trace("Disjoint: " +  Collections.disjoint(roles, params.getRoles()));
			}
			return ! Collections.disjoint(roles, params.getRoles());
		}
		return true;
	}
	
	protected boolean allowed(ParamsBean params, String role) {
		return allowed(params, Collections.singleton(role));
	}
}
