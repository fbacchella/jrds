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
	};

	public abstract boolean check(ParamsBean params);	
	public abstract ACL join(ACL acl);
}
