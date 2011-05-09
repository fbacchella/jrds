package jrds.probe;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.starter.Connection;
import jrds.starter.SocketFactory;
import jrds.starter.Starter;
import junit.framework.Assert;
import jrds.starter.Resolver;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApacheHttpClientTest {
    static final private Logger logger = Logger.getLogger(ApacheHttpClientTest.class);

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        logger.setLevel(Level.TRACE);
        Tools.setLevel(new String[] {HCHttpProbe.class.getName(),HttpClientStarter.class.getName(), Resolver.class.getName(), Connection.class.getName(), "jrds.Starter" }, logger.getLevel());
    }

    private  RdsHost addConnection(Starter cnx) {
        RdsHost localhost = new RdsHost("localhost");
        localhost.registerStarter(new SocketFactory());

        PropertiesManager pm = new PropertiesManager();
        pm.timeout = 1;
        pm.collectorThreads = 1;
        localhost.registerStarter(cnx);
        cnx.configure(pm);
        return localhost;
    }

    @Test
    public void testStart() {

        HttpClientStarter cnx = new HttpClientStarter();
        RdsHost localhost = addConnection(cnx);
        localhost.find(Resolver.class).doStart();
        logger.debug("resolver started for localhost:" + localhost.find(Resolver.class).isStarted());
        cnx.doStart();
        Assert.assertTrue("Apache HttpClient failed to start" , cnx.isStarted());

        cnx.stop();
    }

    @Test
    public void testConnect() throws MalformedURLException {
        HttpClientStarter cnx = new HttpClientStarter();
        RdsHost localhost = addConnection(cnx);
        localhost.find(Resolver.class).doStart();
        cnx.doStart();
        HCHttpProbe p = new HCHttpProbe() {
            @Override
            protected boolean checkStoreFile() {
                return true;
            }

            @Override
            public String getName() {
                return "ApacheHttpClient";
            }

            @Override
            protected Map<String, Number> parseStream(InputStream stream) {
                return Collections.emptyMap();
            }

        };
        p.setName("toto");
        p.setPd(new ProbeDesc());
        p.setHost(localhost);
        p.configure(new URL("http://localhost:4141"));
        p.checkStore();
        localhost.getProbes().add(p);
        localhost.collectAll();
    }

}
