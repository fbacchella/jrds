/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.snmp;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.snmp4j.PDU;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;


/**
 * An abstract class to generate simple SNMP requesters
 * which gets a probe, a collection of ois
 * and make snmp requests based on those oid 
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public abstract class SnmpRequester {
	static private final Logger logger = Logger.getLogger(SnmpRequester.class);

	/**
	 * No constructor, full static class;
	 */
	private SnmpRequester() {};

	public abstract String getName();

	/**
	 * The method that need to be implemented to do the request
	 * @param probe the probe that does the request
	 * @param oidsSet a <code>collection</code> to be request
	 * @return a map of the snmp values read
	 * @throws IOException 
	 */
	public abstract Map<OID, Object> doSnmpGet(SnmpStarter starter, Collection<OID> oidsSet) throws IOException;

	/**
	 * Collect a set of variable by append .0 to the OID of the oid
	 * the returned OID are left unchanged
	 */
	public static final SnmpRequester  SIMPLE = new SnmpRequester() {
		public Map<OID, Object> doSnmpGet(SnmpStarter starter, Collection<OID> oidsSet) throws IOException
		{
			VariableBinding[] vars = new VariableBinding[oidsSet.size()];
			int j = 0;
			for(OID i:  oidsSet) {
				OID currentOid = (OID) i.clone();
				currentOid.append("0");
				vars[j++] = new VariableBinding(currentOid);
			}
			return doRequest(starter, vars);
		}

		@Override
		public String getName() {
			return "simple";
		}
	};

	/**
	 *  A requester used to read an array of oid
	 */
	public static final SnmpRequester TABULAR = new SnmpRequester() {

		@SuppressWarnings("unchecked")
		public Map<OID, Object> doSnmpGet(SnmpStarter starter, Collection<OID> oids)
		{
			SnmpVars retValue = new SnmpVars();

			if(starter != null && starter.isStarted()) {
				Target snmpTarget = starter.getTarget();
				if(snmpTarget != null) {
					DefaultPDUFactory localfactory = new DefaultPDUFactory();
					TableUtils tableRet = new TableUtils(starter.getSnmp(), localfactory);
					tableRet.setMaxNumColumnsPerPDU(30);
					OID[] oidTab= new OID[oids.size()];
					oids.toArray(oidTab);
					for(TableEvent te: (Iterable<TableEvent>)tableRet.getTable(snmpTarget, oidTab, null, null)) {
						if(! te.isError()) {
							retValue.join(te.getColumns());
						}
					}
				}
			}
			return retValue;
		}

		@Override
		public String getName() {
			return "tabular";
		}	
	};

	/**
	 * The simplest requester
	 * Just get a collection of oid and return the associated value
	 */
	public static final SnmpRequester RAW = new SnmpRequester () {

		public Map<OID, Object> doSnmpGet(SnmpStarter starter, Collection<OID> oidsSet) throws IOException
		{
			VariableBinding[] vars = new VariableBinding[oidsSet.size()];
			int j = 0;
			for(OID currentOid: oidsSet) {
				vars[j++] = new VariableBinding(currentOid);
			}
			return doRequest(starter, vars);
		}

		@Override
		public String getName() {
			return "raw";
		}
	};

	private static final Map<OID, Object> doRequest(SnmpStarter starter, VariableBinding[] vars) throws IOException {
		Map<OID, Object> snmpVars = new SnmpVars();

		Target snmpTarget = starter.getTarget();
		PDU requestPDU = DefaultPDUFactory.createPDU(snmpTarget, PDU.GET);

		requestPDU.addAll(vars);

		boolean doAgain = true;
		PDU response = null;
		do {
			ResponseEvent re = null;
			if(requestPDU.size() > 0) {
				re = starter.getSnmp().send(requestPDU, snmpTarget);
			}
			if(re != null)
				response = re.getResponse();
			if (response != null && response.getErrorStatus() == SnmpConstants.SNMP_ERROR_SUCCESS){
				snmpVars = new SnmpVars(response);
				doAgain = false;
			}	
			else {		
				if(response == null) {
					throw new IOException("SNMP Timeout, address=" + snmpTarget.getAddress() + ", requestID=" + requestPDU.getRequestID());
				}
				else {
					int index = response.getErrorIndex() - 1;
					VariableBinding vb = response.get(index);
					logger.warn(response.getErrorStatusText() + " on " + vb.getOid().toString());
					/*If there is still variable to get, we try again*/
					if(requestPDU.size() > 1) {
						requestPDU = response;
						response = null;
						requestPDU.remove(index);
						requestPDU.setType(PDU.GET);
					}
					else
						doAgain = false;
				}
			}
		} while (doAgain);
		return snmpVars;

	}
}