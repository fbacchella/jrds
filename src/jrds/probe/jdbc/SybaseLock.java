package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.ProbeDesc;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseLock extends Sybase {

	private static final ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("db_size", ProbeDesc.GAUGE);
		pd.add("data", ProbeDesc.GAUGE);
		pd.add("log", ProbeDesc.GAUGE);
		pd.add("process", ProbeDesc.GAUGE);
		pd.add("data+log", ProbeDesc.GAUGE);
		pd.add("free_log", ProbeDesc.GAUGE);
		//pd.setGraphClasses(new Class[] { SybaseGraph.class });
	}

	static {
		registerDriver("com.sybase.jdbc2.jdbc.SybDriver");
	}

	/**
	 * 
	 */
	public SybaseLock(String dbName, String user, String passwd) {
		super(dbName, user, passwd);
		setName("sybsp-" + dbName);
	}

	/**
	 * 
	 */
	public SybaseLock(Integer port, String dbName, String user, String passwd) {
		super(port,  dbName, user, passwd);
		setName("sybsp-" + dbName);
	}

	@Override
	public List<String> getQueries() {
		List<String> retValues = new ArrayList<String>(3);
		retValues.add("select (lct_admin('logsegment_freepages', db_id('" + getDbName() + "')) - lct_admin('reserved_for_rollbacks', db_id('ADSLV7'))) * @@maxpagesize free_log");
		retValues.add("select count(*) process from master..sysprocesses where suid > 0");
		retValues.add(			
				"select count(1)\n" +
				"from master..syslocks l, master..spt_values v1, master..spt_values v2\n" +
				"where l.type = v1.number\n" +
				"and v1.type = 'L'\n" +
				"and (l.context+2049) = v2.number\n" +
				"and v2.type = 'L2'\n" +
				"and db_name(dbid) = '" + getDbName() + "'\n"
		);
		return retValues;
	}

	@Override
	public Map<String, Number> parseRs(ResultSet rs) throws SQLException {
		return (Map<String, Number>) this.parseRsHorizontaly(rs, true);
	}
}
