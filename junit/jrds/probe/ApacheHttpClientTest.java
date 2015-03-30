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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;

public class ApacheHttpClientTest {
    static final private Logger logger = Logger.getLogger(ApacheHttpClientTest.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private boolean collected = false;
    private Server server = null;
    private boolean shouldFail = true;

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, HCHttpProbe.class.getName(), HttpClientStarter.class.getName(), Resolver.class.getName(), Connection.class.getName(), "jrds.Starter", "jrds.Probe.ApacheHttpClientTester");
        StoreOpener.prepare("FILE");
    }

    @Before
    public void startServer() throws Exception {

        server = new Server(0);
        ServerConnector connector = new ServerConnector(server);
        server.setConnectors(new Connector[]{connector});

        ResourceHandler staticFiles = new ResourceHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                    HttpServletRequest request, HttpServletResponse response)
                            throws IOException, ServletException {
                if(shouldFail) {
                    response.setStatus(404);                    
                } else {
                    response.setStatus(200);                    
                }
                response.getOutputStream().println("an empty line");
                response.flushBuffer();
            }
        };

        HandlerCollection handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{staticFiles});
        server.setHandler(handlers);
        server.start();

    }

    @After
    public void finish() throws Exception {
        server.stop();
        server = null;
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
    public void testStart() throws IOException {
        HttpClientStarter cnx = new HttpClientStarter();
        HostStarter localhost = addConnection(cnx);
        localhost.find(SSLStarter.class).doStart();
        localhost.find(Resolver.class).doStart();
        logger.debug("resolver started for localhost:" + localhost.find(Resolver.class).isStarted());
        cnx.doStart();
        Assert.assertTrue("Apache HttpClient failed to start" , cnx.isStarted());
        cnx.stop();
    }

    @Test
    public void testConnect() throws IOException, InterruptedException {
        HttpClientStarter cnx = new HttpClientStarter();
        HostStarter localhost = addConnection(cnx);
        localhost.find(Resolver.class).doStart();
        cnx.doStart();
        HCHttpProbe p = new HCHttpProbe() {
            @Override
            public String getName() {
                return "ApacheHttpClient";
            }
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                ApacheHttpClientTest.this.collected = true;
                return Collections.emptyMap();
            }
        };
        Map<String, String> empty = Collections.emptyMap();
        p.setMainStore(new RrdDbStoreFactory(), empty);
        p.setName("toto");
        ProbeDesc pd = new ProbeDesc();
        pd.add("test", DsType.COUNTER);
        pd.setName("ApacheHttpClientTester");
        p.setPd(pd);
        p.setHost(localhost);
        p.configure(server.getURI().toURL());
        p.checkStore();
        localhost.addProbe(p);
        localhost.getParent().startCollect();
        // Run twice, to detect failure management in the probe
        localhost.collectAll();
        Thread.sleep(1500);
        shouldFail = false;
        localhost.collectAll();
        Assert.assertTrue("Didn't try to collect", collected);
    }

}
