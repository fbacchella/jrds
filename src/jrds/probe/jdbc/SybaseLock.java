package jrds.probe.jdbc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.RdsHost;

import org.apache.log4j.Logger;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseLock extends JdbcProbe {
	static final private org.apache.log4j.Logger logger = Logger.getLogger(SybaseLock.class);
	private final static String urlPrefix = "jdbc:sybase:Tds:";
	private final static int SEGMENT_SYSTEM = 1;
	private final static int SEGMENT_USER = 2;
	private final static int SEGMENT_SYSUSER = 3;
	private final static int SEGMENT_LOG = 4;
	private final static int SEGMENT_DATALOG = 7;
	
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
	
	private String dbName;
	
	/**
	 * 
	 */
	public SybaseLock(RdsHost thehost, String dbName, String user, String passwd) {
		super(urlPrefix, thehost, pd, 4100, user, passwd);
		this.dbName = dbName;
		setName("sybsp-" + dbName);
	}
	
	/**
	 * 
	 */
	public SybaseLock(RdsHost thehost, Integer port, String dbName, String user, String passwd) {
		super(urlPrefix, thehost, pd, port.intValue(),  user, passwd);
		this.dbName = dbName;
		setName("sybsp-" + dbName);
	}
	
	protected String doUrl()
	{
		return urlPrefix +  getHost() + ":" + this.getPort() + "/" + dbName;
	}
	
	/*   select fid, spid, loid, locktype = v1.name, table_name = object_name(id, dbid), page, row, 
	 dbname = db_name(dbid), class, context=v2.name 
	 from master..syslocks l, master..spt_values v1, master..spt_values v2 
	 where l.type = v1.number 
	 and v1.type = 'L' 
	 and (l.context+2049) = v2.number 
	 and v2.type = 'L2'
	 and db_name(dbid) = 'CONSOV7'
	 order by fid, spid, loid, dbname, table_name, page, row, locktype 
	 */
	
	public Map getNewSampleValues()
	{
		Map retValue = new HashMap(pd.getSize());
		String jdbcurl = getJdbcurl();
		logger.debug("Getting number of lock " + jdbcurl); 
		
		List logs = select2Map(
				"select (lct_admin('logsegment_freepages', db_id('" + dbName + "')) - lct_admin('reserved_for_rollbacks', db_id('ADSLV7'))) * @@maxpagesize free_log");
		//int logfree = ((Number)((Map)logs.get(0)).get("free_log")).intValue();
		retValue.putAll((Map)logs.get(0));
		
		List process = select2Map("select count(*) process from master..sysprocesses where suid > 0");
		retValue.putAll((Map)process.get(0));
		
		List segments = select2Map(				
				"select count(1)\n" +
				"from master..syslocks l, master..spt_values v1, master..spt_values v2\n" +
				"where l.type = v1.number\n" +
				"and v1.type = 'L'\n" +
				"and (l.context+2049) = v2.number\n" +
				"and v2.type = 'L2'\n" +
				"and db_name(dbid) = '" + dbName + "'\n"
				, true);
		
		closeDbCon();
		return retValue;
	}
	
	/**
	 * @return Returns the dbName.
	 */
	public String getDbName() {
		return dbName;
	}
}
