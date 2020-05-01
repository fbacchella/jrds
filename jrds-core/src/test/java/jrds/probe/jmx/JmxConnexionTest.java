package jrds.probe.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.jolokia.config.ConfigKey;
import org.jolokia.jvmagent.JolokiaServer;
import org.jolokia.jvmagent.JolokiaServerConfig;
import org.jolokia.util.LogHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import com.sun.net.httpserver.HttpServer;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.probe.JMX;
import jrds.probe.JMXConnection;
import jrds.probe.JmxSocketFactory;
import jrds.starter.HostStarter;
import jrds.starter.SocketFactory;
import jrds.starter.StarterNode;

@SuppressWarnings("restriction")
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
    
    @MXBean
    public interface TestMBean {
        default public long getFailure() throws RemoteException {
            throw new RemoteException("failed", new RuntimeException("Original failed"));
        }
    }

    public static class Implementation extends StandardMBean implements TestMBean {
        public Implementation()
                        throws NotCompliantMBeanException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException {
            super(TestMBean.class);
        }

    }

    @BeforeClass
    public static void registerCustom() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("jrds", "type", JmxConnexionTest.class.getCanonicalName());
        mbs.registerMBean(new Implementation(), name);
    }

    public static class JolokiaLogHandler implements LogHandler {
        private static Logger logger;
        @Override
        public void debug(String message) {
            logger.debug(message);
        }

        @Override
        public void info(String message) {
            logger.info(message);
        }

        @Override
        public void error(String message, Throwable t) {
            logger.error(message);
        }
        
    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, JmxConnexionTest.class.getName(), JMXConnection.class.getName(), 
                         "jrds.Starter", "javax.management", "sun.rmi", "jrds.probe.jmx", "org.jolokia", "jrds.Probe.EmptyProbe");
        JolokiaLogHandler.logger = logrule.getTestlogger();
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

    private void doTest(String proto, Consumer<Integer> start, Runnable stop) throws Exception {
        StarterNode top = new StarterNode() {
            @Override
            public boolean isCollectRunning() {
                return true;
            }
            @Override
            public Logger getInstanceLogger() {
                return logrule.getTestlogger();
            }
        };
        top.setTimeout(1);
        int port = findPort();
        Assert.assertTrue("can't allocate port " + port, port >= 32767);
        start.accept(port);
        try {
            HostInfo hi = new HostInfo("localhost");
            hi.setDnsName(InetAddress.getLoopbackAddress().getCanonicalHostName());
            HostStarter host = new HostStarter(hi);
            JMXConnection cnx = getCnx(proto, port);
            host.setParent(top);
            host.registerStarter(new SocketFactory(1));
            host.registerStarter(new JmxSocketFactory());
            host.registerStarter(cnx);
            host.configureStarters(new PropertiesManager());
            top.startCollect();
            Assert.assertTrue("JMX Connection failed to start", host.startCollect());
            Assert.assertTrue("JMX Connection failed to start",
                              cnx.isStarted());
            Assert.assertTrue("Failed to read uptime", cnx.setUptime() >= 0);
            JMX probe = new JMX();
            collect(true, cnx, probe, "java.lang:type=Runtime/Uptime");
            collect(false, cnx, probe, "java.lang:type=Runtime/Uptimea");
            collect(false, cnx, probe, "java.langa:type=Runtime/Uptime");
            collect(true, cnx, probe, "java.lang:type=OperatingSystem/ProcessCpuTime");
            collect(true, cnx, probe, "java.lang:type=MemoryPool,name=Compressed Class Space/Usage/used");
            collect(true, cnx, probe, "java.nio:type=BufferPool,name=direct/MemoryUsed");
            collect(false, cnx, probe, String.format("jrds:type=%s/Failure", JmxConnexionTest.class.getCanonicalName()));
        } finally {
            stop.run();
        }
    }
    
    private void collect(boolean expected, JMXConnection cnx, JMX probe, String collect) throws MalformedObjectNameException {
        Number n = cnx.getConnection().collect(probe, collect);
        if (expected) {
            Assert.assertNotNull(collect + " not found", n);
        } else {
            Assert.assertNull(collect + " with unexpected value", n);
        }
    }

    private void testNative(String proto) throws Exception {
        CompletableFuture <JrdsMBeanInfo> fmbi = new CompletableFuture<>();
        doTest(proto, p -> {
            JrdsMBeanInfo mbi;
            try {
                mbi = new JrdsMBeanInfo(proto, "localhost", p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            fmbi.complete(mbi);
        }, () -> {
            Assert.assertTrue(fmbi.isDone());
            try {
                JrdsMBeanInfo mbi = fmbi.get();
                mbi.cc.close();
                mbi.cs.stop();
                if(mbi.rmiRegistry != null) {
                    UnicastRemoteObject.unexportObject(mbi.rmiRegistry, true);
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test(timeout=5000)
    public void ConnectJmxmpTest() throws Exception {
        testNative("jmxmp");
    }

    @Test(timeout=5000)
    public void ConnectRmiTest() throws Exception {
        testNative("rmi");
    }

    @Test(timeout=5000)
    public void ConnectJolokiaTest() throws Exception {
        CompletableFuture <JolokiaServer> fj = new CompletableFuture<>();
        CompletableFuture <HttpServer> fh = new CompletableFuture<>();
        doTest("jolokia", p -> {
            InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), p);
            HttpServer httpserver;
            try {
                httpserver = HttpServer.create(socketAddress, p);
                httpserver.start();
                Map<String,String> config = new HashMap<>();
                config.put("port", Integer.toString(p));
                config.put(ConfigKey.DISCOVERY_ENABLED.getKeyValue(), "false");
                config.put("debug", "true");
                config.put(ConfigKey.LOGHANDLER_CLASS.getKeyValue(), JolokiaLogHandler.class.getName());
                config.put(ConfigKey.ALLOW_ERROR_DETAILS.getKeyValue(), "true");
                config.put(ConfigKey.INCLUDE_STACKTRACE.getKeyValue(), "true");
                config.put(ConfigKey.SERIALIZE_EXCEPTION.getKeyValue(), "true");
                JolokiaServer server = new JolokiaServer(httpserver, new JolokiaServerConfig(config), false);
                server.start();
                fj.complete(server);
                fh.complete(httpserver);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, () -> {
            Assert.assertTrue(fj.isDone());
            Assert.assertTrue(fh.isDone());
            try {
                fj.get().stop();
                fh.get().stop(0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        });
    }

}
