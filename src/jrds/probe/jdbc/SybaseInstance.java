package jrds.probe.jdbc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.probe.IndexedProbe;


/**
 * @author bacchell
 *
 * TODO 
 */
public class SybaseInstance extends JdbcProbe implements IndexedProbe {
	static final private org.apache.log4j.Logger logger = JrdsLogger.getLogger(SybaseInstance.class.getPackage().getName());
	private final static String urlPrefix = "jdbc:sybase:Tds:";
	private final static int SEGMENT_SYSTEM = 1;
	private final static int SEGMENT_USER = 2;
	private final static int SEGMENT_SYSUSER = 3;
	private final static int SEGMENT_LOG = 4;
	private final static int SEGMENT_DATALOG = 7;
	
	private static final ProbeDesc pd = new ProbeDesc(5);
	static {
		pd.add("process", ProbeDesc.GAUGE);
		pd.add("transactions", ProbeDesc.GAUGE);
		pd.setGraphClasses(new Object[] {"sybaseinstance.xml"});
	}
	
	static {
		registerDriver(com.sybase.jdbc2.jdbc.SybDriver.class);
	}

	/**
	 * 
	 */
	public SybaseInstance(RdsHost thehost, String user, String passwd) {
		super(urlPrefix, thehost, pd, 4100, user, passwd);
		setName("sybase-" + getPort());
	}

	/**
	 * 
	 */
	public SybaseInstance(RdsHost thehost, Integer port, String user, String passwd) {
		super(urlPrefix, thehost, pd, port.intValue(),  user, passwd);
		setName("sybase-" + getPort());
	}
	
	protected String doUrl()
	{
		return urlPrefix +  getHost() + ":" + getPort() + "/master";
	}

	public Map getNewSampleValues()
	{
		Map retValue = new HashMap(pd.getSize());
		String jdbcurl = getJdbcurl();
		logger.debug("Getting activity of " + jdbcurl); 

		List transactions = select2Map("select count(*) transactions from master..syslogshold");
		retValue.putAll((Map)transactions.get(0));
		List process = select2Map("select count(*) process from master..sysprocesses where suid > 0");
		retValue.putAll((Map)process.get(0));

		closeDbCon();
		return retValue;
	}

	/* (non-Javadoc)
	 * @see jrds.probe.IndexedProbe#getIndexName()
	 */
	public String getIndexName() {
		return new String("" + getPort());
	}
}
