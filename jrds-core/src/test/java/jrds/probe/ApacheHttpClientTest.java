package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.StoreOpener;
import jrds.Tools;
import jrds.mockobjects.MockHttpServer;
import jrds.starter.Connection;
import jrds.starter.HostStarter;
import jrds.starter.Resolver;
import jrds.starter.SSLStarter;
import jrds.starter.SocketFactory;
import jrds.starter.Starter;
import jrds.starter.Timer;
import jrds.store.RrdDbStoreFactory;

public class ApacheHttpClientTest {

    static private final Map<String, String> empty = Collections.emptyMap();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    private volatile boolean shouldFail = true;
    private final ResourceHandler staticFiles = new ResourceHandler() {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if(ApacheHttpClientTest.this.shouldFail) {
                response.setStatus(404);
            } else {
                response.setStatus(200);
            }
            response.getOutputStream().println("an empty line");
            response.flushBuffer();
        }
    };

    private static class TestHttpProbe extends HCHttpProbe<String> {
        boolean collected;

        public TestHttpProbe() {
            ProbeDesc<String> pd = new ProbeDesc<>();
            pd.setName("ApacheHttpClientTester");
            pd.add("test", DsType.COUNTER);
            setPd(pd);
        }

        @Override
        public String getName() {
            return "ApacheHttpClient";
        }

        @Override
        protected Map<String, Number> parseStream(InputStream stream) {
            collected = true;
            return Collections.emptyMap();
        }
    }

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        StoreOpener.prepare("FILE");
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, HCHttpProbe.class.getName(), HttpClientStarter.class.getName(), Resolver.class.getName(), Connection.class.getName(), "jrds.Starter", "jrds.Probe.ApacheHttpClientTester");
        logrule.setLevel(Level.INFO, "org.eclipse", "org.apache");
    }

    private HostStarter addConnection(Starter cnx) throws IOException {
        return addConnection(cnx, "localhost", true);
    }

    private HostStarter addConnection(Starter cnx, String hostname, boolean start) throws IOException {
        String truststore = getClass().getClassLoader().getResource("localhost.jks").getFile();
        PropertiesManager pm = Tools.makePm(testFolder, "collectorThreads=1",
                                            "ssl.protocol=TLSv1.2", "ssl.strict=true", "ssl.truststore=" + truststore, "ssl.trustpassword=123456");
        HostStarter localhost = new HostStarter(new HostInfo(hostname));
        Timer t = Tools.getDefaultTimer();
        localhost.setParent(t);
        localhost.getHost().setHostDir(testFolder.getRoot());
        t.registerStarter(new SSLStarter());
        t.registerStarter(new SocketFactory(1));
        t.configureStarters(pm);
        if (start) {
            localhost.find(Resolver.class).doStart();
            localhost.find(SSLStarter.class).doStart();
        }
        localhost.registerStarter(cnx);
        cnx.configure(pm);
        return localhost;
    }

    @Test
    public void parseArgs() throws IOException, InvocationTargetException {
        HttpClientStarter cnx = new HttpClientStarter();
        HostStarter localhost = addConnection(cnx, "jrds.fr", false);
        TestHttpProbe p = new TestHttpProbe();
        p.setMainStore(new RrdDbStoreFactory(), empty);
        p.setHost(localhost);
        p.setPort(8080);
        p.setScheme("ftp");
        p.setFile("${1}/${host}");
        p.setLogin("login");
        p.setPassword("password");
        p.configure(Collections.singletonList("/file"));
        Assert.assertEquals("ftp://jrds.fr:8080/file/jrds.fr", p.getUrlAsString());
    }

    @Test
    public void testStart() throws Exception {
        try (MockHttpServer server = new MockHttpServer(false)) {
            server.addResourceHandler(staticFiles);
            server.start();

            HttpClientStarter cnx = new HttpClientStarter();
            HostStarter localhost = addConnection(cnx);
            logger.debug("resolver started for localhost:" + localhost.find(Resolver.class).isStarted());
            cnx.doStart();
            Assert.assertTrue("Apache HttpClient failed to start", cnx.isStarted());
            cnx.stop();
        }
    }

    @Test
    public void testConnectedConnexion() throws Exception {
        try (MockHttpServer server = new MockHttpServer(false)) {
            server.addResourceHandler(staticFiles);
            server.start();
            HttpClientStarter cnx = new HttpClientStarter();
            HostStarter localhost = addConnection(cnx);
            HttpClientConnection serverconnexion = new HttpClientConnection();
            serverconnexion.setPort(server.getURI().toURL().getPort());
            serverconnexion.setName("serverconnexion");
            localhost.registerStarter(serverconnexion);
            localhost.find(Resolver.class).doStart();
            cnx.doStart();
            serverconnexion.doStart();
            TestHttpProbe p = new TestHttpProbe();
            p.setMainStore(new RrdDbStoreFactory(), empty);
            p.setHost(localhost);
            p.setConnectionName("serverconnexion");
            p.configure();
            p.checkStore();
            localhost.addProbe(p);
            localhost.getParent().startCollect();
            shouldFail = false;
            localhost.collectAll();
            Assert.assertTrue("Didn't try to collect", p.collected);
        }
    }

    @Test
    public void testConnectTwice() throws Exception {
        try (MockHttpServer server = new MockHttpServer(false)) {
            server.addResourceHandler(staticFiles);
            server.start();
            HttpClientStarter cnx = new HttpClientStarter();
            HostStarter localhost = addConnection(cnx);
            localhost.find(Resolver.class).doStart();
            cnx.doStart();
            TestHttpProbe p = new TestHttpProbe();
            p.setMainStore(new RrdDbStoreFactory(), empty);
            p.setHost(localhost);
            p.setPort(server.getURI().toURL().getPort());
            p.configure();
            p.checkStore();
            localhost.addProbe(p);
            localhost.getParent().startCollect();
            // Run twice, to detect failure management in the probe
            localhost.collectAll();
            shouldFail = false;
            Thread.sleep(1500);
            localhost.collectAll();
            Assert.assertTrue("Didn't try to collect", p.collected);
        }
    }

    @Test
    public void testConnectSSL() throws Exception {
        try (MockHttpServer server = new MockHttpServer(true)) {
            server.addResourceHandler(staticFiles);
            server.start();

            HttpClientStarter cnx = new HttpClientStarter();
            HostStarter localhost = addConnection(cnx);
            cnx.doStart();
            TestHttpProbe p = new TestHttpProbe();
            p.setMainStore(new RrdDbStoreFactory(), empty);
            p.setHost(localhost);
            p.setPort(server.getURI().toURL().getPort());
            p.setScheme("https");
            p.configure();
            p.checkStore();
            localhost.addProbe(p);
            localhost.getParent().startCollect();
            Assert.assertTrue(p.find(SSLStarter.class).isStarted());
            shouldFail = false;
            localhost.collectAll();
            Assert.assertTrue("Didn't try to collect", p.collected);
        }
    }

}
