package jrds.snmp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.VariableBinding;

public class TestSnmpVars {
    @Test
    public void conversionUnsignedInteger32() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("1");
        VariableBinding vb = new VariableBinding(oid1, new UnsignedInteger32(1));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), (long) 1);
    }
    @Test
    public void conversionInteger32() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("2");
        VariableBinding vb = new VariableBinding(oid1, new Integer32(1));
        vars.addVariable(vb);
        Assert.assertEquals(1, vars.get(oid1));
    }
    @Test
    public void conversionCounter32() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("3");
        VariableBinding vb = new VariableBinding(oid1, new Counter32(1));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), (long) 1);
    }
    @Test
    public void conversionCounter64() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("4");
        VariableBinding vb = new VariableBinding(oid1, new Counter64(1));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), (long) 1);
    }
    @Test
    public void conversionGauge32() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("5");
        VariableBinding vb = new VariableBinding(oid1, new Gauge32(1));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), (long) 1);
    }
    @Test
    public void conversionNull() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("6");
        VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.Null());
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), null);
    }
    @Test
    public void conversionTimeTicks() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("7");
        VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.TimeTicks(1));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), (double) 0.01);
    }
    @Test
    public void conversionOctetString() {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("8");
        VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.OctetString("a"));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), "a");
    }
    @Test
    public void conversionIpAddress() throws UnknownHostException {
        jrds.snmp.SnmpVars vars = new jrds.snmp.SnmpVars();
        OID oid1 = new OID("9");
        VariableBinding vb = new VariableBinding(oid1, new org.snmp4j.smi.IpAddress("127.0.0.1"));
        vars.addVariable(vb);
        Assert.assertEquals(vars.get(oid1), InetAddress.getByName("127.0.0.1"));
    }

}
