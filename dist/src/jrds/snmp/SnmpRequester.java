/*
 * Created on 23 déc. 2004
 *
 * TODO 
 */
package jrds.snmp;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import jrds.JrdsLogger;
import jrds.SnmpProbe;

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
  */
public abstract class SnmpRequester {
	static private final DefaultPDUFactory factory = new DefaultPDUFactory();
	static private final Logger logger = JrdsLogger.getLogger(SnmpRequester.class.getPackage().getName());
	static private Snmp snmp;
	static private boolean started = false;

	private SnmpRequester() {};
	
	/**
	 * The method that need to be implemented to do the request
	 * @param probe the probe that does the request
	 * @param oidsSet a <code>collection</code> to be request
	 * @return a map of the snmp values read
	 */
	public abstract SnmpVars doSnmpGet(SnmpProbe probe, Collection oidsSet);
	
	/**
	 * Collect a set of variable by append .0 to the OID of the oid
	 * the returned OID are left unchanged
	 */
	public static final SnmpRequester  SIMPLE = new SnmpRequester() {
		
		public SnmpVars doSnmpGet(SnmpProbe probe, Collection oidsSet)
		{
			SnmpVars snmpVars = null;
			Target snmpTarget = probe.getSnmpTarget();
			if(snmpTarget != null) {
				VariableBinding[] vars = new VariableBinding[oidsSet.size()];
				int j = 0;
				for(Iterator i = oidsSet.iterator(); i.hasNext();) {
					OID currentOid = (OID) ((OID) i.next()).clone();
					currentOid.append("0");
					vars[j++] = new VariableBinding(currentOid);
				}
				snmpVars = doRequest(probe.getSnmpTarget(), vars);
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
		
		public SnmpVars doSnmpGet(SnmpProbe probe, Collection oids)
		{
			Target snmpTarget = probe.getSnmpTarget();
			SnmpVars retValue = new SnmpVars();
			if(snmpTarget != null) {
				TableUtils tableRet = new TableUtils(snmp, factory);
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
		
		public SnmpVars doSnmpGet(SnmpProbe probe, Collection oidsSet)
		{
			SnmpVars snmpVars = null;
			Target snmpTarget = probe.getSnmpTarget();
			if(snmpTarget != null) {
				VariableBinding[] vars = new VariableBinding[oidsSet.size()];
				int j = 0;
				for(Iterator i = oidsSet.iterator(); i.hasNext();) {
					OID currentOid = (OID) i.next();
					vars[j++] = new VariableBinding(currentOid);
				}
				snmpVars = doRequest(probe.getSnmpTarget(), vars);
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
	public static final void start() throws IOException {
		snmp = new Snmp(new DefaultUdpTransportMapping());
		//snmp.addTransportMapping(new DefaultTcpTransportMapping());
		
		/*MPv3 mpv3 =
		 (MPv3)snmp.getMessageProcessingModel(MessageProcessingModel.MPv3);
		 USM usm = new USM(SecurityProtocols.getInstance(),
		 new OctetString(mpv3.createLocalEngineID()), 0);
		 SecurityModels.getInstance().addSecurityModel(usm);*/
		
		snmp.listen();
		
		started = true;
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
	
	private static final SnmpVars doRequest(Target snmpTarget, VariableBinding[] vars) {
		SnmpVars snmpVars = null;
		factory.setPduType(PDU.GET);
		PDU requestPDU = factory.createPDU(snmpTarget);
		requestPDU.addAll(vars);
		try {
			boolean doAgain = true;
			PDU response = null;
			do {
				ResponseEvent re = null;
				if(requestPDU.size() > 0)
					re = snmp.send(requestPDU, snmpTarget);
				if(re != null)
					response = re.getResponse();
				if (response != null && response.getErrorStatus() == SnmpConstants.SNMP_ERROR_SUCCESS ){
					snmpVars = new SnmpVars(response);
					doAgain = false;
				}	
				else {		
					if(response == null) {
						logger.warn("SNMP Timeout for host " + snmpTarget.getAddress());
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
			logger.warn("SNMP communication problem with host " + snmpTarget.getAddress() + ": " + e.getLocalizedMessage());	
		}
	return snmpVars;

	}
}