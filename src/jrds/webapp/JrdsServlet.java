package jrds.webapp;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import jrds.HostsList;
import jrds.PropertiesManager;

public abstract class JrdsServlet extends HttpServlet {

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
	
	protected boolean allowed(ParamsBean params) {
		if(getPropertiesManager().security) {
			return jrds.Util.rolesAllowed(getHostsList().getDefaultRoles(), params.getRoles());
		}
		return false;
	}
}
