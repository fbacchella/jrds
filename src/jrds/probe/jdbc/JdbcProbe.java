package jrds.probe.jdbc;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.RdsHost;
import jrds.Util;
import jrds.probe.IndexedProbe;
import jrds.probe.UrlProbe;

import org.apache.log4j.Logger;

public abstract class JdbcProbe extends Probe implements UrlProbe, IndexedProbe {
	static final private org.apache.log4j.Logger logger = Logger.getLogger(JdbcProbe.class);

	static final void registerDriver(String JdbcDriver) {
		try {
			Driver jdbcDriver = (Driver) Class.forName (JdbcDriver).newInstance();
			DriverManager.registerDriver(jdbcDriver);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	static final void registerDriver(Class JdbcDriver) {
		try {
			Driver jdbcDriver = (Driver) JdbcDriver.newInstance();
			DriverManager.registerDriver(jdbcDriver);
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	private int port;
	protected JdbcStarter starter;

	public JdbcProbe(int port, String user, String passwd) {
		this.port = port;
		starter = setStarter();
		starter.setPasswd(passwd);
		starter.setUser(user);
	}

	public JdbcProbe(int port, String user, String passwd, String dbName) {
		this.port = port;
		starter = setStarter();
		starter.setDbName(dbName);
		starter.setPasswd(passwd);
		starter.setUser(user);
	}

	@Override
	public String getName() {
		return "jdbc-" + Util.stringSignature(getUrlAsString());
	}

	abstract JdbcStarter setStarter();

	public Map getNewSampleValues()
	{
		Map<String, Number> retValue = new HashMap<String, Number>(getPd().getSize());

		for(String query: getQueries())
			retValue.putAll(select2Map(query));
		return retValue;
	}

	public abstract List<String> getQueries();

	protected List<Map<String, Object>> parseRsVerticaly(ResultSet rs, boolean numFilter) throws SQLException {
		ArrayList<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int colCount = rsmd.getColumnCount();
		logger.debug("Columns: " + colCount);
		while(rs.next())  {
			Map<String, Object> row = new HashMap<String, Object>(colCount);
			String key =  rs.getObject(1).toString();
			Object oValue = rs.getObject(2).toString();
			row.put(key, oValue);
			values.add(row);
		}
		values.trimToSize();
		return values;
	}

	protected List<Map<String, Object>> parseRsHorizontaly(ResultSet rs, boolean numFilter) throws SQLException {
		ArrayList<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int colCount = rsmd.getColumnCount();
		while(rs.next()) {
			Map<String, Object> row = new HashMap<String, Object>(colCount);
			for (int i = 1; i <= colCount; i++) {
				String key = rsmd.getColumnLabel(i);
				Object oValue = rs.getObject(i);
				if(numFilter) {
					if(oValue instanceof Number)
						oValue = (Number) oValue;
					else
						oValue = new Double(Double.NaN);
				}
				row.put(key, oValue);
			}
			values.add(row);
		}
		values.trimToSize();
		return values;
	}

	public abstract Map<String, Number> parseRs(ResultSet rs) throws SQLException;

	/**
	 * Parse all the collumns of a query and return a List of Map
	 * where the column name is the key
	 * @param query
	 * @param numFilter force all value to be a Number
	 * @return a List of Map of values 
	 */
	public Map<String, Number> select2Map(String query)
	{
		Map<String, Number> values = new HashMap<String, Number>();
		String jdbcurl = getUrlAsString();
		logger.debug("Getting " + query + " on "  + jdbcurl); 
		try {
			Statement stmt = starter.getStatment();
			if(stmt.execute(query)) {
				do {
					values = parseRs(stmt.getResultSet());
				} while(stmt.getMoreResults());
			}
			else {
				logger.warn("Not a select query");
			}
			stmt.close();
		} catch (SQLException e) {
			logger.error("Error with " + jdbcurl + ": " + e, e);
		}
		return values;
	}

	public Map select2Map(String query, String keyCol, String valCol)
	{
		Map<String, Object> values = new HashMap<String, Object>();
		String jdbcurl = getUrlAsString();
		logger.debug("Getting " + query + " on "  + jdbcurl); 
		Statement stmt = null;
		try {
			stmt = starter.getStatment();
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
		return values;
	}


	@Override
	public void setHost(RdsHost monitoredHost) {
		super.setHost(monitoredHost);
		starter.setHost(monitoredHost);
		starter = (JdbcStarter) monitoredHost.addStarter(starter);
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
	 * @return Returns the dbName.
	 */
	public String getDbName() {
		return starter.getDbName();
	}

	public String getUrlAsString() {
		return starter.getUrlAsString();
	}

	@Override
	public boolean isStarted() {
		return starter.isStarted();
	}

	@Override
	public String getSourceType() {
		return "JDBC";
	}

	@Override
	public long getUptime() {
		return starter.getUptime();
	}

	public String getIndexName() {
		return starter.getDbName();
	}
	
}
