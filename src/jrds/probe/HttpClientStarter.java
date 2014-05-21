package jrds.probe;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLContext;

import jrds.PropertiesManager;
import jrds.starter.SSLStarter;
import jrds.starter.SocketFactory;
import jrds.starter.Starter;

import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Level;

public class HttpClientStarter extends Starter {
    private static final String USERAGENT = "JRDS HTTP agent";

    private CloseableHttpClient client = null;
    private int maxConnect = 0;
    private Class<? extends AbstractVerifier> verifier = AllowAllHostnameVerifier.class;

    /* (non-Javadoc)
     * @see jrds.starter.Starter#configure(jrds.PropertiesManager)
     */
    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        maxConnect = pm.numCollectors;
    }

    @Override
    public boolean start() {

        HttpClientBuilder builder = HttpClientBuilder.create();

        RegistryBuilder<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create();

        // Register http and his plain socket factory
        final SocketFactory ss = getLevel().find(SocketFactory.class);
        ConnectionSocketFactory plainsf = new PlainConnectionSocketFactory() {
            @Override
            public Socket createSocket(HttpContext context) throws IOException {
                return ss.createSocket();
            }
        };
        r.register("http", plainsf);

        // Register https
        r.register("https", getSSLSocketFactory());

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r.build());        
        cm.setMaxTotal(maxConnect * 2);
        cm.setDefaultMaxPerRoute(2);
        builder.setConnectionManager(cm);

        builder.setUserAgent(USERAGENT);

        client = builder.build();

        return true;
    }

    private final SSLConnectionSocketFactory getSSLSocketFactory() {
        AbstractVerifier verifierInstance = null;
        try {
            verifierInstance = verifier.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("failed to set a SSL context", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("failed to set a SSL context", e);
        }
        SSLStarter sslstarter = getLevel().find(SSLStarter.class);
        SSLContext sc = sslstarter.getContext();
        return new SSLConnectionSocketFactory(sc, sslstarter.getSupportedProtocols(), sslstarter.getSupportedCipherSuites(), verifierInstance);
    }

    @Override
    public void stop() {
        try {
            client.close();
        } catch (IOException e) {
            log(Level.ERROR, "http client closed failed: %s", e.getMessage());
        }
        client = null;
    }

    public HttpClient getHttpClient() {
        return client;
    }

    @Override
    public boolean isStarted() {
        return client != null && getLevel().find(SSLStarter.class).isStarted();
    }
}
