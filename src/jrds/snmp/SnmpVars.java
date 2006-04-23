/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds.snmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.snmp4j.PDU;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
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
public class SnmpVars extends HashMap {
	static final private Logger logger = Logger.getLogger(SnmpVars.class);

	static final private byte TAG1 = (byte) 0x9f;
	static final private byte TAG_FLOAT = (byte) 0x78;
	static final private byte TAG_DOUBLE = (byte) 0x79;

	public SnmpVars(PDU data) {
		super(data.size());
		join(data);
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
	 * @param vb
	 */
	@SuppressWarnings("unchecked")
	public void addVariable(VariableBinding vb)
	{
		if( ! vb.isException()) {
			OID vbOid = vb.getOid();
			put(vbOid, convertVar(vb.getVariable()));
		}
		else {
			logger.warn("OID " + vb.getOid() + " has error");
		}
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
		for (int i = 0 ; i < newVars.length ; i++)
			addVariable(newVars[i]);
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
					long epochcentisecond = valueAsVar.toLong();//((UnsignedInteger32)valueAsVar).getValue();
					retvalue  = new Double(epochcentisecond / 100.0 );
				}
				else
					retvalue  = valueAsVar.toLong(); //new Long(((UnsignedInteger32)valueAsVar).getValue());
			}
			else if(valueAsVar instanceof Integer32)
				retvalue  = valueAsVar.toInt(); //new Integer(((Integer32)valueAsVar).getValue());
			else if(valueAsVar instanceof Counter64)
				retvalue  = valueAsVar.toLong(); //new Long(((Counter64)valueAsVar).getValue());
			else if(valueAsVar instanceof OctetString) {
				if(valueAsVar instanceof Opaque) {
					retvalue  = resolvOpaque((Opaque) valueAsVar);
				}
				else
					retvalue  = valueAsVar.toString(); //((OctetString)valueAsVar).toString();
			}
			else if(valueAsVar instanceof Null) {
				retvalue  = null;
			}
			else if(valueAsVar instanceof IpAddress) {
				retvalue  = ((IpAddress)valueAsVar).getInetAddress();
			}
			else {
				logger.warn("Unknown syntax " + Variable.getSyntaxString(type));
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
}
