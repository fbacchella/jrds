/*
 * Created on 3 févr. 2005
 *
 * TODO 
 */
package jrds.probe.snmp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jrds.JrdsLogger;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.RdsSnmpSimple;
import jrds.graphe.ProcessStatusGraph;
import jrds.snmp.SnmpRequester;
import jrds.snmp.SnmpVars;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;


/**
 * @author bacchell
 *
 * TODO 
 */
public class ProcessStatus extends RdsSnmpSimple {
	static final private Logger logger = JrdsLogger.getLogger(ProcessStatus.class.getPackage().getName());
	static final private OID psProcessState = new OID(".1.3.6.1.4.1.42.3.12.1.1.5");
	static final private String RUNNABLE="R";
	static final private String STOPPED="T";
	static final private String INPAGEWAIT="P";
	static final private String NONINTERRUPTABLEWAIT="D";
	static final private String SLEEPING="S";
	static final private String IDLE="I";
	static final private String ZOMBIE="Z";

	static final private ProbeDesc pd = new ProbeDesc(7);
	static {
		pd.add("psProcessState", psProcessState);
		pd.add(RUNNABLE, ProbeDesc.GAUGE);
		pd.add(STOPPED, ProbeDesc.GAUGE);
		pd.add(INPAGEWAIT, ProbeDesc.GAUGE);
		pd.add(NONINTERRUPTABLEWAIT, ProbeDesc.GAUGE);
		pd.add(SLEEPING, ProbeDesc.GAUGE);
		pd.add(IDLE, ProbeDesc.GAUGE);
		pd.add(ZOMBIE, ProbeDesc.GAUGE);
		pd.setGraphClasses(new Class []{ProcessStatusGraph.class});
		pd.setRrdName("pslist");
	}

	/**
	 * @param monitoredHost
	 */
	public ProcessStatus(RdsHost monitoredHost) {
		super(monitoredHost, pd);
	}

	protected SnmpRequester getSnmpRequester() {
		return SnmpRequester.TABULAR;
	}

	/* (non-Javadoc)
	 * @see com.aol.jrds.snmp.SnmpStore#storeValues(com.aol.jrds.snmp.SnmpVars, org.jrobin.core.Sample, com.aol.jrds.RdsSnmpRrd)
	 */
	public Map filterValues(SnmpVars snmpVars){
		int runnable = 0;
		int stopped = 0;
		int inPageWait = 0;
		int nonInterruptableWait = 0;
		int sleeping = 0;
		int idle = 0;
		int zombie = 0;
		for(Iterator i = snmpVars.keySet().iterator() ; i.hasNext() ; ){
			OID oid = (OID) i.next();
			String state = (String) snmpVars.get(oid);
			if(RUNNABLE.equals(state))
				runnable++;
			else if(STOPPED.equals(state))
				stopped++;
			else if(INPAGEWAIT.equals(state))
				inPageWait++;
			else if(NONINTERRUPTABLEWAIT.equals(state))
				nonInterruptableWait++;
			else if(SLEEPING.equals(state))
				sleeping++;
			else if(IDLE.equals(state))
				idle++;
			else if(ZOMBIE.equals(state))
				zombie++;
			
		}
		Map retValue = new HashMap(7);
		retValue.put(RUNNABLE, new Double(runnable));
		retValue.put(STOPPED, new Double(stopped));
		retValue.put(INPAGEWAIT, new Double(inPageWait));
		retValue.put(NONINTERRUPTABLEWAIT, new Double(nonInterruptableWait));
		retValue.put(SLEEPING, new Double(sleeping));
		retValue.put(IDLE, new Double(idle));
		retValue.put(ZOMBIE, new Double(zombie));
		return retValue;
	}

}
