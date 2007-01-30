/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.PDU;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;



/**
 * A extension to a an HashMap, it's main purpose is to be constructed from an snmp pdu and
 * return values as java base objects
 * the key to access a value is it's OID
 * It supports float and double stored in opaque value
 *
 *  @author Fabrice Bacchella
 */
public class SnmpVars extends HashMap<OID, Object> {
	static final private Logger logger = Logger.getLogger(SnmpVars.class);

	static final private byte TAG1 = (byte) 0x9f;
	static final private byte TAG_FLOAT = (byte) 0x78;
	static final private byte TAG_DOUBLE = (byte) 0x79;

	private final Map<OID, Integer> errors = new HashMap<OID, Integer>(0);

	public SnmpVars(PDU data) {
		super(data.size());
		join(data);
	}

	public SnmpVars(VariableBinding[] newVars) {
		super(newVars.length);
		join(newVars);
	}

	/**
	 *
	 */
	public SnmpVars() {
		super();
	}

	public SnmpVars(int initialCapacity) {
		super(initialCapacity);
	}

	/** Add directly a VariableBinding to the map
	 * it will be stored as a key/value and the original snmp datas will be lost
	 * only not 
	 * @param vb
	 */
	@SuppressWarnings("unchecked")
	public boolean addVariable(VariableBinding vb)
	{
		boolean retValue = false;
		if( ! vb.isException()) {
			OID vbOid = vb.getOid();
			put(vbOid, convertVar(vb.getVariable()));
			retValue = true;
		}
		else {
			errors.put(vb.getOid(), vb.getSyntax());
			int exception = vb.getSyntax();
			String exceptionName = "";
			switch(exception) {
			case SMIConstants.EXCEPTION_END_OF_MIB_VIEW:
				exceptionName = "End of mib view"; break;
			case SMIConstants.EXCEPTION_NO_SUCH_INSTANCE:            
				exceptionName = "No such instance"; break;
			case SMIConstants.EXCEPTION_NO_SUCH_OBJECT: 
				exceptionName = "No such object"; break;
			default: exceptionName = "Unknown exception";break;
			}
			logger.trace("Exception " +  exceptionName + " for " + vb.getOid());
		}
		return retValue;
	}

	public boolean isError(OID tocheck) {
		return errors.containsKey(tocheck);
	}

	public Map<OID, Integer> getErrors() {
		return errors;
	}

	public void join(PDU data)
	{
		for(int i = 0 ; i < data.size() ; i++) {
			VariableBinding vb = data.get(i);
			addVariable(vb);
		}
	}

	public void join(VariableBinding[] newVars)
	{
		for (int i = 0 ; i < newVars.length ; i++) {
			addVariable(newVars[i])	;
		}
	}

	private Object convertVar(Variable valueAsVar) {
		Object retvalue = null;
		if (valueAsVar != null) {
			int type = valueAsVar.getSyntax();
			if( valueAsVar instanceof OID) {
				retvalue = valueAsVar;
			}
			else if(valueAsVar instanceof UnsignedInteger32) {
				if(valueAsVar instanceof TimeTicks) {
					long epochcentisecond = valueAsVar.toLong();
					retvalue  = new Double(epochcentisecond / 100.0 );
				}
				else
					retvalue  = valueAsVar.toLong();
			}
			else if(valueAsVar instanceof Integer32)
				retvalue  = valueAsVar.toInt();
			else if(valueAsVar instanceof Counter64)
				retvalue  = valueAsVar.toLong();
			else if(valueAsVar instanceof OctetString) {
				if(valueAsVar instanceof Opaque) {
					retvalue  = resolvOpaque((Opaque) valueAsVar);
				}
				else
					retvalue  = valueAsVar.toString();
			}
			else if(valueAsVar instanceof Null) {
				retvalue  = null;
			}
			else if(valueAsVar instanceof IpAddress) {
				retvalue  = ((IpAddress)valueAsVar).getInetAddress();
			}
			else {
				logger.warn("Unknown syntax " + AbstractVariable.getSyntaxString(type));
			}
		}
		return retvalue;
	}

	private final Object resolvOpaque(Opaque var) {

		//If not resolved, we will return the data as an array of bytes
		Object value = var.getValue();

		try {
			byte[] bytesArray = var.getValue();
			ByteBuffer bais = ByteBuffer.wrap(bytesArray);
			BERInputStream beris = new BERInputStream(bais);
			byte t1 = bais.get();
			byte t2 = bais.get();
			int l = BER.decodeLength(beris);
			if(t1 == TAG1) {
				if(t2 == TAG_FLOAT && l == 4)
					value = new Float(bais.getFloat());
				else if(t2 == TAG_DOUBLE && l == 8)
					value = new Double(bais.getDouble());
			}
		} catch (IOException e) {
			logger.error(var.toString());
		}
		return value;
	}

	@Test public void conversionUnsignedInteger32() {
		OID oid1 = new OID("1");
		VariableBinding vb = new VariableBinding(oid1, new UnsignedInteger32(1));
		addVariable(vb);
		Assert.assertEquals(get(oid1), (long) 1);
	}
	@Test public void conversionInteger32() {
		OID oid1 = new OID("2");
		VariableBinding vb = new VariableBinding(oid1, new UnsignedInteger32(1));
		addVariable(vb);
		Assert.assertEquals(get(oid1), (long) 1);
	}
	@Test public void conversionCounter32() {
		OID oid1 = new OID("3");
		VariableBinding vb = new VariableBinding(oid1, new Counter32(1));
		addVariable(vb);
		Assert.assertEquals(get(oid1), (long) 1);
	}
	@Test public void conversionCounter64() {
		OID oid1 = new OID("4");
		VariableBinding vb = new VariableBinding(oid1, new Counter64(1));
		addVariable(vb);
		Assert.assertEquals(get(oid1), (long) 1);
	}
	@Test public void conversionGauge32() {
		OID oid1 = new OID("5");
		VariableBinding vb = new VariableBinding(oid1, new Gauge32(1));
		addVariable(vb);
		Assert.assertEquals(get(oid1), (long) 1);
	}
	@Test public void conversionNull() {
		OID oid1 = new OID("6");
		VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.Null());
		addVariable(vb);
		Assert.assertEquals(get(oid1), null);
	}
	@Test public void conversionTimeTicks() {
		OID oid1 = new OID("7");
		VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.TimeTicks(1));
		addVariable(vb);
		Assert.assertEquals(get(oid1), (double) 0.01);
	}
	@Test public void conversionOctetString() {
		OID oid1 = new OID("7");
		VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.OctetString("a"));
		addVariable(vb);
		Assert.assertEquals(get(oid1), "a");
	}
	@Test public void conversionIpAddress() {
		OID oid1 = new OID("8");
		VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.IpAddress("127.0.0.1"));
		addVariable(vb);
		try {
			Assert.assertEquals(get(oid1), InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			Assert.fail(e.getMessage());
		}
	}
}
