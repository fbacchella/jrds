package thresolds;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class DismanPlay {
	static final private Logger logger = Logger.getLogger(DismanPlay.class);

	OID snmpTrapOID    				 = new OID("1.3.6.1.6.3.1.1.4.1.0");
	OID sysUpTime				     = new OID("1.3.6.1.2.1.1.3");
	OID dismanEventNotificationGroup = new OID("1.3.6.1.2.1.88.3.2.6");
	OID mteTriggerFired              = new OID("1.3.6.1.2.1.88.2.0.1");
	OID mteTriggerRising             = new OID("1.3.6.1.2.1.88.2.0.2");
	OID mteTriggerFalling            = new OID("1.3.6.1.2.1.88.2.0.3");
	//"The name of the trigger causing the notification."
	//OCTET STRING (0..255)
	OID mteHotTrigger				 = new OID("");
	//The SNMP Target MIB's snmpTargetAddrName related to the notification.
	//OCTET STRING (0..255)
	OID mteHotTargetName			 = new OID("");
	//The context name related to the notification.  This MUST be as
    //fully-qualified as possible, including filling in wildcard
    //information determined in processing.
	//OCTET STRING (0..255)
	OID mteHotContextName			 = new OID("");
	//The object identifier of the destination object related to the
    //notification.  This MUST be as fully-qualified as possible,
    //including filling in wildcard information determined in
    //processing.
    //
    //For a trigger-related notification this is from
    //mteTriggerValueID.
    //
    //For a set failure this is from mteEventSetObject.
	//OBJECT IDENTIFIER
	OID mteHotOID					 = new OID("");
	//The value of the object at mteTriggerValueID when a
    //trigger fired.
	//Integer32
	OID mteHotValue					 = new OID("");
	
	@BeforeClass
	static public void configure() throws ParserConfigurationException, IOException {
		Tools.configure();
		Tools.setLevel(new String[] {"jrds.Util"}, logger.getLevel());
	}

	@Test
	public void send() throws IOException {
		Address targetAddress = GenericAddress.parse("udp:132.192.22.151/162");
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("public"));
		target.setAddress(targetAddress);
		target.setVersion(SnmpConstants.version2c);

		PDU pdu = new PDU();
		pdu.setType(PDU.TRAP);

		add(pdu, snmpTrapOID, mteTriggerFired);
		add(pdu, snmpTrapOID, new TimeTicks(1000));

		@SuppressWarnings("rawtypes")
        TransportMapping transport = new DefaultUdpTransportMapping();
		transport.listen();
		Snmp snmp = new Snmp(transport);
		
		@SuppressWarnings("unused")
        ResponseEvent respEv = snmp.send(pdu, target);
		//PDU response = respEv.getResponse();
	}
	
	private void add(PDU pdu, OID key, Object o) {
		Variable v = null;
		if(o instanceof Variable) {
			v = (Variable) o;
		}
		VariableBinding vb = new VariableBinding();
		
		vb.setOid(key);
		vb.setVariable(v);
		pdu.add(vb);
	}
}
