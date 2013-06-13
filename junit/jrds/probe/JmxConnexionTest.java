package jrds.probe;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import jrds.HostInfo;
import jrds.JrdsLoggerConfiguration;
import jrds.JuliToLog4jHandler;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.starter.HostStarter;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmxConnexionTest {
    static final private int JMX_PORT = 8998;
    static final private Logger logger = Logger.getLogger(JmxConnexionTest.class);
    static private final class JrdsMBeanInfo {
        MBeanServer mbs;
        JMXServiceURL url;
        JMXConnector cc;
        JMXConnectorServer cs;
        Registry rmiRegistry = null;

        public JrdsMBeanInfo(String protocol, String host, int port) throws Exception {
            String path = "/";

            if (protocol == "rmi") {
                rmiRegistry = java.rmi.registry.LocateRegistry.createRegistry(port);
                path = "/jndi/rmi://" + host + ":" + port + "/jmxrmi";
            }
            url = new JMXServiceURL(protocol, host, port, path);
            mbs = ManagementFactory.getPlatformMBeanServer();
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();
            JMXServiceURL addr = cs.getAddress();
            cc = JMXConnectorFactory.connect(addr);
        }
    }

    private JrdsMBeanInfo mbi;

    static private final void enumerate(MBeanServerConnection mbean) throws InstanceNotFoundException, IntrospectionException, AttributeNotFoundException, ReflectionException, MBeanException, IllegalArgumentException, IOException {
        Set<ObjectInstance> s = mbean.queryMBeans(null, null);
        for(ObjectInstance o : s) {
            logger.debug("Class: " + o.getClassName());
        }

        for(String domains: mbean.getDomains()) {
            logger.debug("Domains: " + domains);
        }

        for(Object nameObject: mbean.queryNames(null, null)) {
            ObjectName name = (ObjectName) nameObject;
            logger.debug(name);
            MBeanInfo info = mbean.getMBeanInfo(name);
            MBeanAttributeInfo[] attrs = info.getAttributes();
            for(MBeanAttributeInfo attr : attrs) {
                if("javax.management.openmbean.TabularData".equals(attr.getType())) {
                    TabularData td = (TabularData) mbean.getAttribute(name, attr.getName());
                    logger.debug("    TabularData["  + td.size() +"] " + attr.getName());

                }
                else if("javax.management.openmbean.CompositeData".equals(attr.getType())) {
                    CompositeData cd = (CompositeData) mbean.getAttribute(name, attr.getName());
                    if(cd != null) {
                        CompositeType ct = cd.getCompositeType();
                        for(Object key: ct.keySet()) {
                            Object value = cd.get((String)key);
                            logger.debug("    "  + "    " + value.getClass().getName() + " " + key);

                        }
                    }
                }
                else if(attr.getType().startsWith("[")) {
                    Object o = mbean.getAttribute(name, attr.getName());
                    if(o == null)
                        continue;
                    logger.debug("    " + o.getClass().getComponentType().getName() + "[" + Array.getLength(o) + "]" + " " + attr.getName());
                }
                else {
                    logger.debug("    " + attr.getType() + " " + attr.getName());
                }
            }
        }

    }

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        logger.setLevel(Level.TRACE);
        Tools.setLevel(new String[] {JmxConnexionTest.class.getName(),jrds.probe.JMXConnection.class.getName(), "jrds.Starter", "sun.management.jmxremote" }, logger.getLevel());
        JrdsLoggerConfiguration.configureLogger("sun.management", Level.TRACE);
        java.util.logging.Logger.getLogger("sun.management").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("sun.management").addHandler(new JuliToLog4jHandler());
    };

    @After
    public void finished() throws Exception {
        mbi.cc.close();
        mbi.cs.stop();
        if(mbi.rmiRegistry != null) {
            UnicastRemoteObject.unexportObject(mbi.rmiRegistry,true);  
        }
    }

    private JMXConnection getCnx(String proto, int port) {
        JMXConnection cnx = new JMXConnection() {
            @Override
            public String getHostName() {
                return "localhost";
            }   
        };
        cnx.setPort(port);
        cnx.setProtocol(proto);
        return cnx;
    }

    @SuppressWarnings("unused")
    private void doTest(String proto, int port) throws Exception {
        mbi = new JrdsMBeanInfo(proto, "localhost", port);

        HostStarter host = new HostStarter(new HostInfo("localhost")) {
            public boolean isCollectRunning() {
                return true;
            }
        };
        JMXConnection cnx = getCnx(proto, port);
        host.registerStarter(cnx);

        host.configureStarters(new PropertiesManager());

        host.startCollect();
        Assert.assertTrue("JMX Connection failed to start", cnx.isStarted());
        Assert.assertNotNull("Failed to read uptime", cnx.setUptime());
        if(false)
            enumerate(cnx.getConnection());
    }

    @Test
    public void ConnectJmxmpTest() throws Exception {
        doTest("jmxmp", JMX_PORT);
    }

    @Test
    public void ConnectRmiTest() throws Exception {
        doTest("rmi", JMX_PORT);
    }

}
