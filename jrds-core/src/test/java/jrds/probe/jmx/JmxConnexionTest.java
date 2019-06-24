package jrds.probe.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.probe.JMXConnection;
import jrds.probe.JmxSocketFactory;
import jrds.starter.HostStarter;
import jrds.starter.SocketFactory;

public class JmxConnexionTest {

    static private final class JrdsMBeanInfo {
        MBeanServer mbs;
        JMXServiceURL url;
        JMXConnector cc;
        JMXConnectorServer cs;
        Registry rmiRegistry = null;

        public JrdsMBeanInfo(String protocol, String host, int port) throws Exception {
            String path = "/";
            if(protocol.equals("rmi")) {
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

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, JmxConnexionTest.class.getName(), JMXConnection.class.getName(), 
                         "jrds.Starter", "javax.management", "sun.rmi");
    }

    @After
    public void finished() throws Exception {
        mbi.cc.close();
        mbi.cs.stop();
        if(mbi.rmiRegistry != null) {
            UnicastRemoteObject.unexportObject(mbi.rmiRegistry, true);
        }
    }

    private int findPort() throws IOException {
        try (ServerSocket  serverSocket = new ServerSocket()){
            serverSocket.setReuseAddress(true);
            InetSocketAddress sa = new InetSocketAddress("localhost", 0);
            serverSocket.bind(sa);
            return serverSocket.getLocalPort();
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

    private void doTest(String proto, int port) throws Exception {
        mbi = new JrdsMBeanInfo(proto, "localhost", port);

        HostStarter host = new HostStarter(new HostInfo("localhost")) {
            public boolean isCollectRunning() {
                return true;
            }
        };
        host.setTimeout(1);
        JMXConnection cnx = getCnx(proto, port);
        host.registerStarter(new SocketFactory());
        host.registerStarter(new JmxSocketFactory());
        host.registerStarter(cnx);

        host.configureStarters(new PropertiesManager());

        host.startCollect();
        Assert.assertTrue("JMX Connection failed to start", cnx.isStarted());
        Assert.assertNotNull("Failed to read uptime", cnx.setUptime());
    }

    @Test(timeout=5000)
    public void ConnectJmxmpTest() throws Exception {
        int port = findPort();
        Assert.assertTrue("can't allocate port " + port, port >= 32767);
        doTest("jmxmp", port);
    }

    @Test(timeout=5000)
    public void ConnectRmiTest() throws Exception {
        int port = findPort();
        Assert.assertTrue("can't allocate port " + port, port >= 32767);
        doTest("rmi", port);
    }

}
