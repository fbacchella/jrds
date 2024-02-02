package jrds.webapp;

import java.util.Collections;
import java.util.Set;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Configuration;
import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.Util;

public abstract class JrdsServlet extends HttpServlet {
    static final private Logger logger = LoggerFactory.getLogger(JrdsServlet.class);

    protected HostsList getHostsList() {
        return Configuration.get().getHostsList();
    }

    protected PropertiesManager getPropertiesManager() {
        return Configuration.get().getPropertiesManager();
    }

    protected ParamsBean getParamsBean(HttpServletRequest request, String... restPath) {
        return new ParamsBean(request, getHostsList(), restPath);
    }

    protected boolean allowed(ParamsBean params, Set<String> roles) {
        if(getPropertiesManager().security) {
            if (params.getRoles().contains(getPropertiesManager().adminrole)) {
                return true;
            } else if (roles.contains("ANONYMOUS")) {
                return true;
            } else {
                logger.trace("Checking if roles {} in roles {}", Util.delayedFormatString(params::getRoles), roles);
                logger.trace("Disjoint: {}", Util.delayedFormatString( () -> Collections.disjoint(roles, params.getRoles())));
                return !Collections.disjoint(roles, params.getRoles());
            }
        }
        return true;
    }

    protected boolean allowed(ParamsBean params, ACL acl, HttpServletRequest req, HttpServletResponse res) {
        if (getPropertiesManager().security) {
            logger.trace("Looking if ACL {} allow access to {}", acl, Util.delayedFormatString(req::getServletPath));
            if (!acl.check(params)) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }

}
