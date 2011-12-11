package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.StoreOpener;
import jrds.Tools;
import jrds.starter.Connection;
import jrds.starter.Resolver;
import jrds.starter.SocketFactory;
import jrds.starter.Starter;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, HCHttpProbe.class.getName(), HttpClientStarter.class.getName(), Resolver.class.getName(), Connection.class.getName(), "jrds.Starter");
        StoreOpener.prepare("FILE");
    }

    private  RdsHost addConnection(Starter cnx) throws IOException {
        PropertiesManager pm = new PropertiesManager();
        pm.put("autocreate", "yes");
        pm.put("timeout", "1");
        pm.put("collectorThreads", "1");
        pm.put("tmpdir", testFolder.getRoot().getCanonicalPath());
        pm.put("rrddir", testFolder.getRoot().getCanonicalPath());
        pm.update();

        RdsHost localhost = new RdsHost("localhost");
        localhost.setHostDir(testFolder.getRoot());
        localhost.registerStarter(new SocketFactory());
        localhost.registerStarter(cnx);
        cnx.configure(pm);
        return localhost;
    }

    @Test
    public void testStart() throws IOException {
        HttpClientStarter cnx = new HttpClientStarter();
        RdsHost localhost = addConnection(cnx);
        localhost.find(Resolver.class).doStart();
        logger.debug("resolver started for localhost:" + localhost.find(Resolver.class).isStarted());
        cnx.doStart();
        Assert.assertTrue("Apache HttpClient failed to start" , cnx.isStarted());
        cnx.stop();
    }

    @Test
    public void testConnect() throws IOException {
        HttpClientStarter cnx = new HttpClientStarter();
        RdsHost localhost = addConnection(cnx);
        localhost.find(Resolver.class).doStart();
        cnx.doStart();
        HCHttpProbe p = new HCHttpProbe() {
            @Override
            public String getName() {
                return "ApacheHttpClient";
            }
            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return Collections.emptyMap();
            }
            public void collect() {
                ApacheHttpClientTest.this.collected = true;
                super.collect();
            }
        };
        p.setName("toto");
        p.setPd(new ProbeDesc());
        p.getPd().add("test", DsType.COUNTER);
        p.setHost(localhost);
        p.configure(new URL("http://localhost:4141"));
        p.checkStore();
        localhost.getProbes().add(p);
        localhost.collectAll();
        Assert.assertTrue("Didn't try to collect", collected);
    }

}
