package jrds.webapp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.Util;

public class RolesACL extends ACL {
    static final private Logger logger = LoggerFactory.getLogger(ACL.class.getName() + ".RolesACL");

    Set<String> roles;

    /**
     * @return the roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    public RolesACL(Set<String> roles) {
        super();
        this.roles = roles;
    }

    public boolean check(ParamsBean params) {
        if(roles.contains("ANONYMOUS"))
            return true;
        if(logger.isTraceEnabled()) {
            logger.trace("Checking if roles {} in roles {}", Util.delayedFormatString(params::getRoles), roles);
            logger.trace("Disjoint: {}", Util.delayedFormatString(() -> Collections.disjoint(roles, params.getRoles())));
        }
        return !Collections.disjoint(roles, params.getRoles());
    }

    @Override
    public ACL join(ACL acl) {
        if(acl instanceof RolesACL) {
            Set<String> newRoles = new HashSet<>(roles);
            newRoles.addAll(((RolesACL) acl).getRoles());
            return new RolesACL(newRoles);
        } else if(acl instanceof AdminACL) {
            Set<String> newRoles = new HashSet<>(roles);
            newRoles.add(((AdminACL) acl).getAdminRole());
            return new RolesACL(newRoles);
        } else {
            return this;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "roles " + roles;
    }

}
