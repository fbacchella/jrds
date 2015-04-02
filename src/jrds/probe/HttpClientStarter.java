package jrds.probe;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import jrds.PropertiesManager;
import jrds.starter.SSLStarter;
import jrds.starter.SocketFactory;
import jrds.starter.Starter;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.Level;

public class HttpClientStarter extends Starter {
    private static final String USERAGENT = "JRDS HTTP agent";

    private CloseableHttpClient client = null;
    private int maxConnect = 0;
    private int timeout = 0;

    /* (non-Javadoc)
     * @see jrds.starter.Starter#configure(jrds.PropertiesManager)
     */
    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        maxConnect = pm.numCollectors;
        timeout  = pm.timeout;
    }

    @Override
    public boolean start() {

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

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setUserAgent(USERAGENT);
        builder.setConnectionTimeToLive(timeout, TimeUnit.SECONDS);
        builder.evictIdleConnections((long)timeout, TimeUnit.SECONDS);
        
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r.build());        
        cm.setMaxTotal(maxConnect * 2);
        cm.setDefaultMaxPerRoute(2);
        cm.setValidateAfterInactivity(timeout * 1000);
        builder.setConnectionManager(cm);

        RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout * 1000)
                .setConnectTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();
        builder.setDefaultRequestConfig(rc);

        client = builder.build();

        return true;
    }

    private SSLConnectionSocketFactory getSSLSocketFactory() {
        SSLStarter sslstarter = getLevel().find(SSLStarter.class);
        SSLContext sc = sslstarter.getContext();
        try {
            sc = SSLContexts.custom()
                    .loadTrustMaterial(new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain,
                                String authType) throws CertificateException {
                            log(Level.TRACE, "trying to check certificates chain %s with authentication method %s", chain, authType);
                            return true;
                        }
                    })
                    .build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("failed to set a SSL context", e);
        }
        return new SSLConnectionSocketFactory(sc, NoopHostnameVerifier.INSTANCE);
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
