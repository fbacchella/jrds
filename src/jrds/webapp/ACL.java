package jrds.webapp;

import org.apache.log4j.Logger;

public abstract class ACL {
	static final protected Logger logger = Logger.getLogger(ACL.class);

	static final public ACL AllowedACL = new ACL(){
		public boolean check(ParamsBean params) {
			return true;
		}

		@Override
		public ACL join(ACL acl) {
			return acl;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "All allowed";
		}
		
	};

	static final public class  AdminACL extends ACL {
		final private String adminRole;
		
		public AdminACL(String adminRole) {
			super();
			this.adminRole = adminRole;
		}
		public String getAdminRole() {
			return adminRole;
		}
		public boolean check(ParamsBean params) {
			return true;
		}

		@Override
		public ACL join(ACL acl) {
			return acl.join(this);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "admin role: \"" + adminRole + "\"";
		}
		
	};

	public abstract boolean check(ParamsBean params);	
	public abstract ACL join(ACL acl);
}
