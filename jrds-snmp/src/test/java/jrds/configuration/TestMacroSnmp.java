package jrds.configuration;
import org.junit.Assert;
import org.junit.Test;

import jrds.HostInfo;
import jrds.Macro;
import jrds.Tools;
import jrds.configuration.HostBuilder;
import jrds.configuration.TestMacro;
import jrds.factories.xml.JrdsDocument;
import jrds.starter.ConnectionInfo;

public class TestMacroSnmp extends TestMacro {
    
    @Test
    public void testMacroStarter() throws Exception {
        JrdsDocument d = Tools.parseString(TestMacro.goodMacroXml);
        d.getRootElement().addElement("snmp", "community=public", "version=2");

        Macro m = TestMacro.doMacro(d, "macrodef");

        HostBuilder hb = getBuilder(m);

        JrdsDocument hostdoc = Tools.parseString(TestMacro.goodHostXml);
        HostInfo host = hb.makeHost(hostdoc);

        ConnectionInfo found = null;
        for(ConnectionInfo ci: host.getConnections()) {
            if("jrds.snmp.SnmpConnection".equals(ci.getName())) {
                found = ci;
                break;
            }
        }
        Assert.assertNotNull("SNMP starter not found", found);
        Assert.assertEquals("Starter not found", "jrds.snmp.SnmpConnection", found.getName());
    }

}
