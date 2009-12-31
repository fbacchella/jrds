package jrds.probe;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import jrds.starter.Connection;

import org.apache.log4j.Logger;

public class LdapConnection extends Connection<DirContext> {
	static final private Logger logger = Logger.getLogger(LdapConnection.class);
	private String binddn;
	private String password;
	private int port = 389;
	DirContext dctx = null;
	
	long uptime;

	public LdapConnection() {
		super();
	}

	public LdapConnection(Integer port) {
		super();
		this.port = port;
	}
	public LdapConnection(Integer port, String binddn, String password) {
		super();
		this.binddn = binddn;
		this.password = password;
		this.port = port;
	}

	public LdapConnection(String binddn, String password) {
		super();
		this.binddn = binddn;
		this.password = password;
	}

	@Override
	public DirContext getConnection() {
		return dctx;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#start()
	 */
	@Override
	public boolean startConnection() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://" + getHostName() +  ":" + port);
		if(binddn != null && password !=null) {
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, binddn);
			env.put(Context.SECURITY_CREDENTIALS, password);
		}
		env.put("com.sun.jndi.ldap.connect.timeout", "" + getTimeout() * 1000);

		try {
			dctx = new InitialDirContext(env);
		} catch (NamingException e) {
			logger.error("Cannot connect to " + getParent() + ", cause: " + e.getCause());
			return false;
		}

		logger.info("Binding to: " + env.get(Context.PROVIDER_URL) + " with dn: " + binddn);
		return dctx != null;
	}

	/* (non-Javadoc)
	 * @see jrds.Starter#stop()
	 */
	@Override
	public void stopConnection() {
		uptime = 1;
		if(dctx != null)
			try {
				dctx.close();
			} catch (NamingException e) {
				logger.error("Error close to " + getParent() + ", cause: " + e.getCause());
			}
			dctx = null;
	}

	@Override
	public long setUptime() {
		return uptime;
	}

}
