/*
 * Created on 8 mars 2005
 *
 * TODO 
 */
package jrds.probe;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class JdbcProbe extends Probe {
	static final private org.apache.log4j.Logger logger = JrdsLogger.getLogger(JdbcProbe.class.getPackage().getName());
	
	static final void registerDriver(String JdbcDriver) {
		try {
			Driver jdbcDriver = (Driver) Class.forName (JdbcDriver).newInstance();
			DriverManager.registerDriver(jdbcDriver);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	private String jdbcurl;
	private String user;
	private String passwd;
	private int port;
	private Connection con;

	/**
	 * @param monitoredHost
	 * @param pd
	 */
	public JdbcProbe(RdsHost monitoredHost, ProbeDesc pd, int port, String user, String passwd) {
		super(monitoredHost, pd);
		this.port = port;
		this.user = user;
		this.passwd = passwd;
	}

	public Map select2Map(String query, String keyCol, String valCol)
	{
		Map values = new HashMap();
		String jdbcurl = getJdbcurl();
		logger.debug("Getting " + query + " on "  + jdbcurl); 
		Statement stmt = null;
		try {
			stmt = getStatment();
			if(stmt.execute(query)) {
				do {
					ResultSet rs = stmt.getResultSet();
					while(rs.next()) {
						String key = rs.getString(keyCol);
						Number value;
						Object oValue = rs.getObject(valCol);
						if(oValue instanceof Number)
							value = (Number) oValue;
						else
							value = new Double(Double.NaN);

						values.put(key, value);
					}
				} while(stmt.getMoreResults());
			}
			stmt.close();
		} catch (SQLException e) {
			logger.error("Error with" + jdbcurl + ": " + e.getLocalizedMessage());
		}
		closeDbCon();
		return values;
	}

	public void openDbCon() throws SQLException {
		if(con == null) 
			con = DriverManager.getConnection(getJdbcurl() , user, passwd);
	}
	
	public void closeDbCon() {
		if(con != null) {
			try {
				con.close();
			} catch (SQLException e2) {
				logger.error("Error with " + getJdbcurl() + ": " + e2.getLocalizedMessage());
			}
		}
		con = null;
	}
	
	public Statement getStatment() throws SQLException {
		if(con == null)
			openDbCon();
		return con.createStatement();
	}

	protected abstract String doUrl();
	
/**
	 * @return Returns the jdbcurl.
	 */
	public String getJdbcurl() {
		if(jdbcurl == null)
			jdbcurl = doUrl();
		return jdbcurl;
	}
	/**
	 * @return Returns the passwd.
	 */
	public String getPasswd() {
		return passwd;
	}
	/**
	 * @param passwd The passwd to set.
	 */
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	/**
	 * @return Returns the port.
	 */
	public int getPort() {
		return port;
	}
	/**
	 * @param port The port to set.
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * @return Returns the user.
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @param user The user to set.
	 */
	public void setUser(String user) {
		this.user = user;
	}
}
