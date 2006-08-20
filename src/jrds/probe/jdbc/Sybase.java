/*
 * Created on 16 déc. 2004
 *
 * TODO 
 */
package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.ProbeDesc;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class Sybase extends JdbcProbe {

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

	/**
	 * 
	 */
	public Sybase(String dbName, String user, String passwd) {
		super(4100, user, passwd);
		setName("syb-" + dbName);
	}

	/**
	 * 
	 */
	public Sybase(Integer port, String dbName, String user, String passwd) {
		super(port.intValue(),  user, passwd);
		setName("syb-" + dbName);
	}
	
	@Override
	JdbcStarter setStarter() {
		return new JdbcStarter() {
			public String getUrlAsString()
			{
				return "jdbc:sybase:Tds:" +  getHost() + ":" + getPort() + "/" + getDbName();
			}
			
		};
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
	
	@Override
	public List<String> getQueries() {
		return java.util.Collections.singletonList("..sp_spaceused");
	}

	@Override
	public Map<String, Number> parseRs(ResultSet rs) throws SQLException {
		Map<String, Number> sizeMap = new HashMap<String, Number>(pd.getSize());
		ResultSetMetaData rsmd = rs.getMetaData();
		int colCount = rsmd.getColumnCount();
		
		for (int i = 1; i <= colCount; i++) {
			String ds = rsmd.getColumnLabel(i);
			String sizeB = (String) rs.getString(i);
			if( ! "database_name".equals(ds))
				sizeMap.put(ds, new Double(trans2bytes(sizeB)));
		}
		return sizeMap;
	}
}
