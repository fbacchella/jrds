/*
 * Created on 16 déc. 2004
 *
 * TODO 
 */
package jrds.probe;

import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.graphe.OracleActivityGraph;
import jrds.graphe.OracleGaugeGraph;


/**
 * @author bacchell
 *
 * TODO 
 */
public class Oracle extends JdbcProbe {
	static final private org.apache.log4j.Logger logger = JrdsLogger.getLogger(Oracle.class);
	private final static String urlPrefix = "jdbc:oracle:thin:";
	
	private static final ProbeDesc pd = new ProbeDesc(11);
	static {
		pd.add("logonscum", ProbeDesc.COUNTER, "logons cumulative");
		pd.add("logonscurr", ProbeDesc.GAUGE,"logons current");
		pd.add("opcurcum", ProbeDesc.COUNTER, "opened cursors cumulative");
		pd.add("opcurscurr", ProbeDesc.GAUGE, "opened cursors current");
		pd.add("usercommit", ProbeDesc.COUNTER, "user commits");
		pd.add("userrollbacks", ProbeDesc.COUNTER, "user rollbacks");
		pd.add("usercalls", ProbeDesc.COUNTER, "user calls");
		pd.add("msgsent", ProbeDesc.COUNTER, "messages sent");
		pd.add("msgrcvd", ProbeDesc.COUNTER, "messages received");
		pd.add("BsSQLNet", ProbeDesc.COUNTER, "bytes sent via SQL*Net to client");
		pd.add("BrSQLNet", ProbeDesc.COUNTER, "bytes received via SQL*Net from client");
		pd.setGraphClasses(new Class[] { OracleActivityGraph.class, OracleGaugeGraph.class });
	}
	static {
		registerDriver(oracle.jdbc.driver.OracleDriver.class);
	}
	
	private String sid;
	

	/**
	 * 
	 */
	public Oracle(RdsHost thehost, String sid, String user, String passwd) {
		super(urlPrefix, thehost, pd, 1521, user, passwd);
		this.sid = sid;
		setRrdName("oracle-" + sid);
	}

	/**
	 * 
	 */
	public Oracle(RdsHost thehost, Integer port, String sid, String user, String passwd) {
		super(urlPrefix, thehost, pd, port.intValue(), user, passwd);
		this.sid = sid;
		setRrdName("oracle-" + sid);
	}
	
	protected String doUrl()
	{
		return urlPrefix +  "@" + getHost() + ":" + getPort() + ":" + sid;
	}


	public Map getNewSampleValues() {
		return select2Map("select NAME,VALUE from V$SYSSTAT", "NAME", "VALUE");
	}
	/**
	 * @return Returns the dbName.
	 */
	public String getSid() {
		return sid;
	}
}
