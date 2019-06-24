package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Macro;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.starter.ConnectionInfo;

public class TestMacroSnmp extends TestMacro {

    @Rule
    public Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.configuration.HostBuilder", "jrds.factories", "jrds.starter.ChainedProperties", "jrds.factories.xml");
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
    }

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
