package jrds.probe.jdbc;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import jrds.starter.Connection;

import org.apache.log4j.Level;

public class JdbcConnection extends Connection<Statement> {

	private java.sql.Connection con;
	private String user;
	private String passwd;
	private String driverClass = null;
	private String url;
	
	public JdbcConnection(String user, String passwd, String url) {
		this.user = user;
		this.passwd = passwd;
		this.url = url;
		checkDriver(url);
	}

	public JdbcConnection(String user, String passwd, String url, String driverClass) {
		this.user = user;
		this.passwd = passwd;
		this.url = url;
		this.driverClass = driverClass;
		checkDriver(url);
	}

	public Statement getConnection() {
		try {
			return con.createStatement();
		} catch (SQLException e) {
			return null;
		}
	}
	
	public void checkDriver(String sqlurl) {
		try {
			if(driverClass != null && ! "".equals(driverClass)) {
				Class.forName(driverClass);
			}
			DriverManager.getDriver(sqlurl);
		} catch (SQLException e) {
			throw new RuntimeException("Error checking JDBC url " + sqlurl, e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Error checking JDBC url " + sqlurl, e);
		}
	}

	@Override
	public long setUptime() {
		return Long.MAX_VALUE;
	}
	
	@Override
	public boolean startConnection() {
		boolean started = false;
		if(getResolver().isStarted()) {
			Properties p = getProperties();
			p.put("user", user);
			p.put("password", passwd);
			String url = getUrl();
			try {
				DriverManager.setLoginTimeout(getTimeout());
				con = DriverManager.getConnection(url , user, passwd);
				started = true;
			} catch (SQLException e) {
				log(Level.ERROR, e, "Sql error for %s: %s" , url, e);
			}
		}
		return started;		
	}

	@Override
	public void stopConnection() {
		if(con != null) {
			try {
				con.close();
			} catch (SQLException e) {
				log(Level.ERROR, e, "Error with %s: %s", getUrl(), e.getMessage());
			}
		}
		con = null;
	}
	
	protected Properties getProperties() {
		return new Properties();
	}

	static protected final void registerDriver(Class<? extends Driver> JdbcDriver) {
		try {
			Driver jdbcDriver = JdbcDriver.newInstance();
			DriverManager.registerDriver(jdbcDriver);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}



	/**
	 * @return the url
	 */
	public String getUrl() {
		return jrds.Util.parseTemplate(url, this, getLevel());
	}

}
