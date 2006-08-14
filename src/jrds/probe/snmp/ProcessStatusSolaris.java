/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.probe.snmp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.snmp4j.smi.OID;


/**
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProcessStatusSolaris extends RdsSnmpSimple {
	static final private String RUNNABLE="R";
	static final private String STOPPED="T";
	static final private String INPAGEWAIT="P";
	static final private String NONINTERRUPTABLEWAIT="D";
	static final private String SLEEPING="S";
	static final private String IDLE="I";
	static final private String ZOMBIE="Z";
	
	/**
	 * @see jrds.Probe#filterValues(java.util.Map)
	 */
	public Map<?, Number> filterValues(Map snmpVars){
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
		Map<String, Number> retValue = new HashMap<String, Number>(7);
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
