/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.snmp;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import jrds.Starter;
import jrds.probe.snmp.SnmpProbe;

import org.apache.log4j.Logger;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
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
	static private Snmp snmp;
	static private boolean started = false;
	static private final OID upTimeOid = new OID(".1.3.6.1.2.1.1.3.0");
	static private final VariableBinding upTimeVb = new VariableBinding(upTimeOid);
	static private final Starter s = new Starter(){

		@Override
		public boolean start() {
			boolean started = false;
			try {
				SnmpRequester.start();
				started = true;
			} catch (IOException e) {
				logger.fatal("Snmp service not started: " + e);
			}
			return started;
		}

		@Override
		public void stop() {
			try {
				SnmpRequester.stop();
			} catch (IOException e) {
				logger.fatal("Snmp service not stopped: " + e);
			}
		}
		
	};
	
	static {
		jrds.HostsList.getRootGroup().addStarter(s);
	}
	/**
	 * No constructor, full static class;
	 */
	private SnmpRequester() {};
	
	
	
	/**
	 * The method that need to be implemented to do the request
	 * @param probe the probe that does the request
	 * @param oidsSet a <code>collection</code> to be request
	 * @return a map of the snmp values read
	 */
	public abstract Map doSnmpGet(SnmpProbe probe, Collection oidsSet);
	
	/**
	 * Collect a set of variable by append .0 to the OID of the oid
	 * the returned OID are left unchanged
	 */
	public static final SnmpRequester  SIMPLE = new SnmpRequester() {
		
		public Map doSnmpGet(SnmpProbe probe, Collection oidsSet)
		{
			Map snmpVars = null;
			Target snmpTarget = probe.getSnmpTarget();
			if(snmpTarget != null) {
				VariableBinding[] vars = new VariableBinding[oidsSet.size()];
				int j = 0;
				for(Iterator i = oidsSet.iterator(); i.hasNext();) {
					OID currentOid = (OID) ((OID) i.next()).clone();
					currentOid.append("0");
					vars[j++] = new VariableBinding(currentOid);
				}
				snmpVars = doRequest(probe, vars);
			}
			else {
				logger.warn("SNMP not configured for the probe " +  probe);		
			}
			
			return snmpVars;
		}
	};
	
	/**
	 *  A requester used to read an array of oid
	 */
	public static final SnmpRequester TABULAR = new SnmpRequester() {
		
		@SuppressWarnings("unchecked")
		public Map doSnmpGet(SnmpProbe probe, Collection oids)
		{
			Target snmpTarget = probe.getSnmpTarget();
			SnmpVars retValue = new SnmpVars();
			if(snmpTarget != null) {
				DefaultPDUFactory localfactory = new DefaultPDUFactory();
				TableUtils tableRet = new TableUtils(snmp, localfactory);
				tableRet.setMaxNumColumnsPerPDU(30);
				OID[] oidTab= new OID[oids.size()];
				oids.toArray(oidTab);
				for(Iterator i = tableRet.getTable(snmpTarget, oidTab, null, null).iterator() ;
				i.hasNext(); ) {
					TableEvent te = (TableEvent) i.next();
					if(! te.isError()) {
						retValue.join(te.getColumns());
					}
				}
			}
			return retValue;
		}	
	};
	
	/**
	 * The simplest requester
	 * Just get a collection of oid and return the associated value
	 */
	public static final SnmpRequester RAW = new SnmpRequester () {
		
		public Map doSnmpGet(SnmpProbe probe, Collection oidsSet)
		{
			Map snmpVars = null;
			Target snmpTarget = probe.getSnmpTarget();
			if(snmpTarget != null) {
				VariableBinding[] vars = new VariableBinding[oidsSet.size()];
				int j = 0;
				for(Iterator i = oidsSet.iterator(); i.hasNext();) {
					OID currentOid = (OID) i.next();
					vars[j++] = new VariableBinding(currentOid);
				}
				snmpVars = doRequest(probe, vars);
			}
			else {
				logger.warn("SNMP not configured for the probe " +  probe);		
			}
			
			return snmpVars;
		}
	};
	
	/**
	 * Start the SNMP environement
	 * should be called only once
	 * @throws IOException
	 */
	public static final Snmp start() throws IOException {
		if( ! started) {
			snmp = new Snmp(new DefaultUdpTransportMapping());
			snmp.listen();
			
			started = true;
		}
		return snmp;
	}
	
	/**
	 * stop an openened snmp environnement
	 * @throws IOException
	 */
	public static final void stop () throws IOException {
		if(started)
			snmp.close();
		snmp = null;
		started = false;
	}
	
	private static final Map doRequest(SnmpProbe probe, VariableBinding[] vars) {
		Target snmpTarget = probe.getSnmpTarget();
		SnmpVars snmpVars = null;
		PDU requestPDU = DefaultPDUFactory.createPDU(snmpTarget, PDU.GET);
		
		requestPDU.addAll(vars);
		
		requestPDU.add(upTimeVb);
		
		try {
			boolean doAgain = true;
			PDU response = null;
			do {
				ResponseEvent re = null;
				if(requestPDU.size() > 0) {
					re = snmp.send(requestPDU, snmpTarget);
				}
				if(re != null)
					response = re.getResponse();
				if (response != null && response.getErrorStatus() == SnmpConstants.SNMP_ERROR_SUCCESS ){
					snmpVars = new SnmpVars(response);
					doAgain = false;
				}	
				else {		
					if(response == null) {
						logger.warn("SNMP Timeout, address=" + snmpTarget.getAddress() + ", requestID=" + requestPDU.getRequestID() + ", probe=" + probe);
						doAgain = false;
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
		} catch (IOException e) {
			logger.warn("SNMP communication problem, address=" + snmpTarget.getAddress() + ", requestID=" + requestPDU.getRequestID() + ": " + e.getLocalizedMessage());	
		}
		return probe.filterUpTime(upTimeOid, snmpVars);
		
	}
}