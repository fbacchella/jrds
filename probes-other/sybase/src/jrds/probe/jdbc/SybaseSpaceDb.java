package jrds.probe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rrd4j.DsType;

import jrds.ProbeDesc;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseSpaceDb extends Sybase {
	private final static int SEGMENT_SYSTEM = 1;
	private final static int SEGMENT_USER = 2;
	private final static int SEGMENT_SYSUSER = 3;
	private final static int SEGMENT_LOG = 4;
	private final static int SEGMENT_DATALOG = 7;
	
	private static final ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("db_size", DsType.GAUGE);
		pd.add("data", DsType.GAUGE);
		pd.add("log", DsType.GAUGE);
		pd.add("process", DsType.GAUGE);
		pd.add("data+log", DsType.GAUGE);
		pd.add("free_log", DsType.GAUGE);
		//pd.setGraphClasses(new Class[] { SybaseGraph.class });
	}

	/**
	 * 
	 */
	public void configure(String dbName, String user, String passwd) {
		super.configure(dbName, user, passwd);
		setName("sybsp-" + dbName);
	}

	/**
	 * 
	 */
	public void configure(Integer port, String dbName, String user, String passwd) {
		super.configure(port, dbName,  user, passwd);
		setName("sybsp-" + dbName);
	}
	

	@Override
	public List<String> getQueries() {
		List<String> queries = new ArrayList<String>(3);
		queries.add("select (lct_admin('logsegment_freepages', db_id('" + getDbName() + "')) - lct_admin('reserved_for_rollbacks', db_id('ADSLV7'))) * @@maxpagesize free_log");
		queries.add("select count(*) process from master..sysprocesses where suid > 0");
		queries.add(
				"declare @numpgsmb 	numeric\n\n" +
				"select @numpgsmb =   v.low\n" +
				"from master.dbo.spt_values v\n" +
				"	 where v.number = 1\n" +
				"	 and v.type = 'E'\n\n" +
				"select u.size * @numpgsmb reserved, \n"+
				"	u.segmap, \n" +
				"	curunreservedpgs(u.dbid, u.lstart, u.unreservedpgs) * @numpgsmb  'free' \n" +
				"from master.dbo.sysusages u, \n" +
				"   master.dbo.sysdevices v, \n" +
				"	master.dbo.spt_values b, \n" +
				"	master.dbo.sysmessages m \n" +
				"where u.dbid = db_id('" + getDbName() + "') \n" +
				"	and vstart between v.low and v.high \n" +
				"	and v.status & 2 = 2 \n" +
				"	and b.type = 'S' " +
				"	and u.segmap & 7 = b.number \n" +
				"	and b.msgnum = m.error\n"
				);
		return null;
	}

	@Override
	public Map<String, Number> parseRs(ResultSet rs) throws SQLException {
		return (Map<String, Number>) this.parseRsHorizontaly(rs, true);
	}

}
