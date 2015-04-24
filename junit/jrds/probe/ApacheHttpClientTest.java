package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jrds.HostInfo;
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;

public class ApacheHttpClientTest {
    static final private Logger logger = Logger.getLogger(ApacheHttpClientTest.class);

    static private final Map<String, String> empty = Collections.emptyMap();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private volatile boolean shouldFail = true;
    private final ResourceHandler staticFiles = new ResourceHandler() {
        @Override
        public void handle(String target, Request baseRequest,
                HttpServletRequest request, HttpServletResponse response)
                        throws IOException, ServletException {
            if(ApacheHttpClientTest.this.shouldFail) {
                response.setStatus(404);                    
            } else {
                response.setStatus(200);                    
            }
            response.getOutputStream().println("an empty line");
            response.flushBuffer();
        }
    };

    private static class TestHttpProbe extends HCHttpProbe {
        boolean collected;

        public TestHttpProbe() {
            ProbeDesc pd = new ProbeDesc();
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
        Tools.setLevel(logger, Level.TRACE, HCHttpProbe.class.getName(), HttpClientStarter.class.getName(), Resolver.class.getName(), Connection.class.getName(), "jrds.Starter", "jrds.Probe.ApacheHttpClientTester");
        StoreOpener.prepare("FILE");
    }

    private  HostStarter addConnection(Starter cnx) throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "timeout=1", "collectorThreads=1");

        HostStarter localhost = new HostStarter(new HostInfo("localhost"));
        Timer t = Tools.getDefaultTimer();
        localhost.setParent(t);
        localhost.getHost().setHostDir(testFolder.getRoot());
        t.registerStarter(new SSLStarter());
        t.registerStarter(new SocketFactory());
        localhost.registerStarter(cnx);
        cnx.configure(pm);
        return localhost;
    }

    @Test
    public void testStart() throws Exception {
        MockHttpServer server = new MockHttpServer(false);
        server.addResourceHandler(staticFiles);
        server.start();

        HttpClientStarter cnx = new HttpClientStarter();
        HostStarter localhost = addConnection(cnx);
        localhost.find(SSLStarter.class).doStart();
        localhost.find(Resolver.class).doStart();
        logger.debug("resolver started for localhost:" + localhost.find(Resolver.class).isStarted());
        cnx.doStart();
        Assert.assertTrue("Apache HttpClient failed to start" , cnx.isStarted());
        cnx.stop();
        server.stop();
    }

    @Test
    public void testConnectTwice() throws Exception {
        MockHttpServer server = new MockHttpServer(false);
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
        server.stop();
    }

    @Test
    public void testConnectSSL() throws Exception {
        MockHttpServer server = new MockHttpServer(true);
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
        p.setScheme("https");
        p.configure();
        p.checkStore();
        localhost.addProbe(p);
        localhost.getParent().startCollect();
        shouldFail = false;
        localhost.collectAll();
        Assert.assertTrue("Didn't try to collect", p.collected);
        server.stop();
    }

}
