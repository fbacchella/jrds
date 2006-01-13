/*
 * Created on 16 déc. 2004
 *
 * TODO 
 */
package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.SybaseGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class Sybase extends JdbcProbe {
	static final private org.apache.log4j.Logger logger = JrdsLogger.getLogger(Sybase.class.getPackage().getName());
	private final static String urlPrefix = "jdbc:sybase:Tds:";
	
	private static final ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("database_size", ProbeDesc.GAUGE);
		pd.add("reserved", ProbeDesc.GAUGE);
		pd.add("data", ProbeDesc.GAUGE);
		pd.add("index_size", ProbeDesc.GAUGE);
		pd.add("unused", ProbeDesc.GAUGE);
		pd.setGraphClasses(new Class[] { SybaseGraph.class });
	}
	
	static {
		registerDriver(com.sybase.jdbc2.jdbc.SybDriver.class);
	}

	private String dbName;

	/**
	 * 
	 */
	public Sybase(RdsHost thehost, String dbName, String user, String passwd) {
		super(urlPrefix, thehost, pd, 4100, user, passwd);
		this.dbName = dbName;
		setRrdName("syb-" + dbName);
	}

	/**
	 * 
	 */
	public Sybase(RdsHost thehost, Integer port, String dbName, String user, String passwd) {
		super(urlPrefix, thehost, pd, port.intValue(),  user, passwd);
		this.dbName = dbName;
		setRrdName("syb-" + dbName);
	}
	
	protected String doUrl()
	{
		return urlPrefix +  getHost() + ":" + this.getPort() + "/" + dbName;
	}

	private double trans2bytes(String line)
	{
		double retValue = 0;
		String[] elems = line.split("\\s+");
		retValue = Double.parseDouble(elems[0]);
		if("KB".equals(elems[1])) {
			retValue *= 1024;
		}
		else if("MB".equals(elems[1])) {
			retValue *= 1024 * 1024;
		}
		
		return retValue;
	}
	
	public Map getNewSampleValues()
	{
		String jdbcurl = getJdbcurl();
		logger.debug("Getting space of database " + jdbcurl); 
		Map sizeMap = new HashMap(pd.getSize());
		Statement stmt = null;
		try {
			stmt = getStatment();
			if(stmt.execute("..sp_spaceused")) {
				do {
					ResultSet rs = stmt.getResultSet();
					ResultSetMetaData rsmd = rs.getMetaData();
					int colCount = rsmd.getColumnCount();
					while(rs.next()) {
						for (int i = 1; i <= colCount; i++) {
							String ds = rsmd.getColumnLabel(i);
							String sizeB = (String) rs.getString(i);
							if( ! "database_name".equals(ds))
								sizeMap.put(ds, new Double(trans2bytes(sizeB)));
						}
					}
				} while(stmt.getMoreResults());
				stmt.close();
			}
		} catch (SQLException e) {
			logger.error("Error with " + jdbcurl + ": " + e.getLocalizedMessage());
		}
		closeDbCon();
		return sizeMap;
	}

	/**
	 * @return Returns the dbName.
	 */
	public String getDbName() {
		return dbName;
	}
}
