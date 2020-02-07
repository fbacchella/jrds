package jrds.probe;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.event.Level;

import jrds.PropertiesManager;
import jrds.starter.SSLStarter;
import jrds.starter.Starter;

public class HttpClientStarter extends Starter {
    private static final String USERAGENT = "JRDS HTTP agent";

    private CloseableHttpClient client = null;
    private int maxConnect = 0;

    /*
     * (non-Javadoc)
     * 
     * @see jrds.starter.Starter#configure(jrds.PropertiesManager)
     */
    @Override
    public void configure(PropertiesManager pm) {
        super.configure(pm);
        maxConnect = pm.numCollectors;
    }

    @Override
    public boolean start() {
        int timeout = getLevel().getTimeout();
        int step = getLevel().getStep();

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setUserAgent(USERAGENT);
        builder.setConnectionTimeToLive(step, TimeUnit.SECONDS);
        builder.evictIdleConnections((long) step, TimeUnit.SECONDS);
        try {
            builder.setSSLContext(getSSLSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            log(Level.ERROR, "No default SSLContext available", e.getMessage());
        }

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
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

    private SSLContext getSSLSocketFactory() throws NoSuchAlgorithmException {
        SSLStarter sslstarter = getLevel().find(SSLStarter.class);
        if (sslstarter == null) {
            return SSLContext.getDefault();
        } else {
            return sslstarter.getContext();
        }
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
        SSLStarter sslfactory = getLevel().find(SSLStarter.class);
        // if no sslfactory, don't check if it's started
        return client != null && (sslfactory != null ? sslfactory.isStarted() : true);
    }

}
