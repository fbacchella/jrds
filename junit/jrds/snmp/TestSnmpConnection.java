package jrds.snmp;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.HostInfo;
import jrds.Tools;
import jrds.factories.ArgFactory;
import jrds.starter.HostStarter;
import jrds.starter.Starter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSnmpConnection {
    static final private Logger logger = Logger.getLogger(TestSnmpConnection.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml();

        Tools.setLevel(logger, Level.TRACE, "jrds.snmp.SnmpConnection", "jrds.RdsHost");
    }

    @Test
    public void testBuild() throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        SnmpConnection cnx = new SnmpConnection();;
        Map<String, PropertyDescriptor> beans = ArgFactory.getBeanPropertiesMap(cnx.getClass(), Starter.class);
        beans.get("community").getWriteMethod().invoke(cnx, "public");
        beans.get("version").getWriteMethod().invoke(cnx, 1);
        
        HostStarter host = new HostStarter(new HostInfo("localhost"));
        host.registerStarter(cnx);
        Assert.assertEquals("SNMP connection not found", "snmp:udp://localhost:161", host.find(SnmpConnection.class).toString());
    }
}
