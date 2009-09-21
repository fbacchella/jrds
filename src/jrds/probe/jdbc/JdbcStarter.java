package jrds.probe.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import jrds.RdsHost;
import jrds.starter.Resolver;
import jrds.starter.Starter;

import org.apache.log4j.Logger;

public abstract class JdbcStarter extends Starter {
	static final private org.apache.log4j.Logger logger = Logger.getLogger(JdbcProbe.class);
	private Starter resolver = null;
	private Connection con;
	private String url;
	private String user;
	private String passwd;
	private String dbName = "";;

	public void setHost(RdsHost monitoredHost) {
		this.url = getUrlAsString();
		resolver = monitoredHost.getStarters().find(Resolver.makeKey(monitoredHost));
	}
	
	public abstract String getUrlAsString();

	@Override
	public boolean start() {
		boolean started = false;
		if(resolver.isStarted()) {
			Properties p = getProperties();
			p.put("user", user);
			p.put("password", passwd);
			try {
				DriverManager.setLoginTimeout(10);
				con = DriverManager.getConnection(url , user, passwd);
				started = true;
			} catch (SQLException e) {
				logger.error("Sql error: " + e + " for " + url);
			}
		}
		return started;
	}

	@Override
	public void stop() {
		if(con != null) {
			try {
				con.close();
			} catch (SQLException e) {
				logger.error("Error with " + url + ": " + e);
			}
		}
		con = null;
	}

	public Statement getStatment() throws SQLException {
		return con.createStatement();
	}

	public Properties getProperties() {
		return new Properties();
	}

	@Override
	public Object getKey() {
		return url;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

}
