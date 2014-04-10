package jrds.probe;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
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
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.starter.HostStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmxConnexionTest {
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
                rmiRegistry = java.rmi.registry.LocateRegistry
                        .createRegistry(port);
                path = "/jndi/rmi://" + host + ":" + port + "/jmxrmi";
            }
            url = new JMXServiceURL(protocol, host, port, path);
            mbs = ManagementFactory.getPlatformMBeanServer();
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null,
                    mbs);
            cs.start();
            JMXServiceURL addr = cs.getAddress();
            cc = JMXConnectorFactory.connect(addr);
        }
    }

    private JrdsMBeanInfo mbi;

    static private void enumerate(MBeanServerConnection mbean) throws InstanceNotFoundException, IntrospectionException, AttributeNotFoundException, ReflectionException, MBeanException, IllegalArgumentException, IOException {
        Set<ObjectInstance> s = mbean.queryMBeans(null, null);
        for(ObjectInstance o : s) {
            logger.debug("Class: " + o.getClassName());
        }

        for(String domains: mbean.getDomains()) {
            logger.debug("Domains: " + domains);
        }

        for(ObjectName name: mbean.queryNames(null, null)) {
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
        Tools.setLevel(new String[] {JmxConnexionTest.class.getName(), JMXConnection.class.getName(), "jrds.Starter"}, logger.getLevel());
    };

    @After
    public void finished() throws Exception {
        mbi.cc.close();
        mbi.cs.stop();
        if(mbi.rmiRegistry != null) {
            UnicastRemoteObject.unexportObject(mbi.rmiRegistry,true);  
        }
    }

    private int findPort() {
        Random r = new Random();
        int port = -1;
        ServerSocket serverSocket = null;
        for(int i=0; i < 10; i++) {
            port = r.nextInt(32767) + 32767;
            try {
                InetSocketAddress sa = new InetSocketAddress("localhost", port);
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(sa);
                break;
            } 
            catch (IOException e) {
            }
            finally {
                if(serverSocket != null)
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                    }
            }
        }
        return port;
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
        int port = findPort();
        Assert.assertTrue("can't allocate port " + port, port >= 32767);
        doTest("jmxmp", port);
    }

    @Test
    public void ConnectRmiTest() throws Exception {
        int port = findPort();
        Assert.assertTrue("can't allocate port " + port, port >= 32767);
        doTest("rmi", port);
    }

}
