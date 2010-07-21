package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;

import jrds.ProbeConnected;
import jrds.ProbeDesc;

public class GenericJdbcProbe extends ProbeConnected<String, Double, JdbcConnection> {
	String query = null;
	String keyColumn = null;

	public GenericJdbcProbe() {
		super(JdbcConnection.class.getName());
	}

	/* (non-Javadoc)
	 * @see jrds.ProbeConnected#configure()
	 */
	public Boolean configure(List<Object> args) {
		if(super.configure()) {
			ProbeDesc pd =  getPd();
			query = jrds.Util.parseTemplate(pd.getSpecific("query"), this, args);
			keyColumn = jrds.Util.parseTemplate(pd.getSpecific("key"), this, args);
			setName(jrds.Util.parseTemplate(pd.getProbeName(), args));
			return true;
		}
		return false;
	}

	@Override
	public Map<String, Double> getNewSampleValuesConnected(JdbcConnection cnx) {
		Map<String, Double> values = Collections.emptyMap();
		Statement stmt = cnx.getConnection();
		try {
			try {
				if(stmt != null && stmt.execute(query)) {
					ResultSet rs = stmt.getResultSet();
					ResultSetMetaData meta = rs.getMetaData();
					int columnCount = meta.getColumnCount();
					values = new HashMap<String, Double>(columnCount);
					while(rs.next()) {
						for(int i = 1; i <= columnCount ; i++) {
							String key = meta.getColumnLabel(i);
							if(keyColumn != null) {
								key = rs.getString(keyColumn) + "." + key;
							}
							double value = Double.NaN;
							Object oValue = rs.getObject(i);
							if(oValue instanceof Number) {
								value = ((Number) oValue).doubleValue();
								values.put(key, value);
							}
							else {
								int type = meta.getColumnType(i);
								switch(type) {
								case Types.BIGINT: value = rs.getBigDecimal(i).doubleValue();
								case Types.DATE: value = rs.getDate(i).getTime();
								case Types.TIME: value = rs.getDate(i).getTime();
								}
								values.put(key, value);
							}
						}
					}
				}
			}
			finally {
				stmt.close();	
			}
		} catch (SQLException e) {
			log(Level.ERROR, e, "SQL exception while getting values: ", e.getMessage());
		}
		return values;
	}

	@Override
	public String getSourceType() {
		return "JDBC";
	}

}
