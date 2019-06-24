package jrds.snmp;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.GenericBean;
import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.ArgFactory;
import jrds.starter.HostStarter;
import jrds.starter.Starter;

public class TestSnmpConnection {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.snmp.SnmpConnection", "jrds.RdsHost");
    }

    @Test
    public void testBuild() throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        SnmpConnection cnx = new SnmpConnection();
        Map<String, GenericBean> beans = ArgFactory.getBeanPropertiesMap(cnx.getClass(), Starter.class);
        beans.get("community").set(cnx, "public");
        beans.get("version").set(cnx, "1");

        HostStarter host = new HostStarter(new HostInfo("localhost"));
        host.registerStarter(cnx);
        Assert.assertEquals("SNMP connection not found", "snmp:udp://localhost:161", host.find(SnmpConnection.class).toString());
    }
}
