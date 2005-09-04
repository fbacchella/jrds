package jrds.probe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseSpaceDb extends JdbcProbe {
	static final private org.apache.log4j.Logger logger = JrdsLogger.getLogger(SybaseSpaceDb.class.getPackage().getName());
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
	public SybaseSpaceDb(RdsHost thehost, String dbName, String user, String passwd) {
		super(urlPrefix, thehost, pd, 4100, user, passwd);
		this.dbName = dbName;
		setRrdName("sybsp-" + dbName);
	}

	/**
	 * 
	 */
	public SybaseSpaceDb(RdsHost thehost, Integer port, String dbName, String user, String passwd) {
		super(urlPrefix, thehost, pd, port.intValue(),  user, passwd);
		this.dbName = dbName;
		setRrdName("sybsp-" + dbName);
	}
	
	protected String doUrl()
	{
		return urlPrefix +  getHost() + ":" + this.getPort() + "/" + dbName;
	}

	public Map getNewSampleValues()
	{
		Map retValue = new HashMap(pd.getSize());
		String jdbcurl = getJdbcurl();
		logger.debug("Getting advanced space of database " + jdbcurl); 

		List logs = select2Map(
				"select (lct_admin('logsegment_freepages', db_id('" + dbName + "')) - lct_admin('reserved_for_rollbacks', db_id('ADSLV7'))) * @@maxpagesize free_log");
		//int logfree = ((Number)((Map)logs.get(0)).get("free_log")).intValue();
		retValue.putAll((Map)logs.get(0));
		
		List process = select2Map("select count(*) process from master..sysprocesses where suid > 0");
		retValue.putAll((Map)process.get(0));
			
		List segments = select2Map(
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
				"where u.dbid = db_id('" + dbName + "') \n" +
				"	and vstart between v.low and v.high \n" +
				"	and v.status & 2 = 2 \n" +
				"	and b.type = 'S' " +
				"	and u.segmap & 7 = b.number \n" +
				"	and b.msgnum = m.error\n"
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
