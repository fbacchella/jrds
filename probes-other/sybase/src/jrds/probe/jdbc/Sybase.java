package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author bacchell
 *
 * TODO 
 */
public abstract class Sybase extends JdbcProbe {
	static final private Logger logger = LoggerFactory.getLogger(Sybase.class);

	static {
		try {
			Class<?> c;
			c = Class.forName("com.sybase.jdbc2.jdbc.SybDriver");
			registerDriver(c);
		} catch (ClassNotFoundException e) {
			logger.fatal("No Sybase jdbc drivers found");
		}
	}

	/**
	 * 
	 */
	public void configure(String dbName, String user, String passwd) {
		super.configure(4100, user, passwd);
		setName("syb-" + dbName);
	}

	/**
	 * 
	 */
	public void configure(Integer port, String dbName, String user, String passwd) {
		super.configure(port.intValue(),  user, passwd);
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
		Map<String, Number> sizeMap = new HashMap<String, Number>(getPd().getSize());
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
