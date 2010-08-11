package jrds.webapp;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

public class RolesACL extends ACL {
	static final private Logger logger = Logger.getLogger(ACL.class.getName() + ".RolesACL");

	List<String> roles;
	
	/**
	 * @return the roles
	 */
	public List<String> getRoles() {
		return roles;
	}

	public RolesACL(List<String> roles) {
		super();
		this.roles = roles;
	}

	public boolean check(ParamsBean params) {
		if(roles.contains("ANONYMOUS"))
			return true;
		if(logger.isTraceEnabled()) {
			logger.trace("Checking if roles " + params.getRoles() + " in roles " + roles);
			logger.trace("Disjoint: " +  Collections.disjoint(roles, params.getRoles()));
		}
		return ! Collections.disjoint(roles, params.getRoles());
	}

	@Override
	public ACL join(ACL acl) {
		if(acl instanceof RolesACL) {
			roles.addAll(((RolesACL) acl).getRoles());
		}
		return this;
	}
	
}
