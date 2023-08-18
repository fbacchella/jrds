package jrds.probe;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.event.Level;

import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Util;
import jrds.starter.SSLStarter;
import jrds.starter.Starter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

public class HttpClientStarter extends Starter {

    @Accessors(chain = true) @ToString
    public static class UrlBuilder {
        @Setter
        private URL url = null;
        @Setter
        private String urlhost = null;
        @Setter
        private int port = -1;
        @Setter
        private String file = "/";
        @Setter
        String scheme = null;
        @Setter
        private String login = null;
        @Setter
        private String password = null;

        private UrlBuilder() {}

        public URL build(Probe<?, ?> p) throws MalformedURLException {
            return build(p, null);
        }

        public URL build(Probe<?, ?> p, List<Object> argslist) throws MalformedURLException {
            URL generatedUrl = null;
            if (url == null) {
                if(port <= 0 && (scheme == null || scheme.isEmpty())) {
                    scheme = "http";
                } else if (scheme == null || scheme.isEmpty()) {
                    if (port == 443) {
                        scheme = "https";
                    } else {
                        scheme = "http";
                    }
                }
                String portString = port < 0 ? "" : (":" + port);
                if (urlhost == null) {
                    urlhost = p.getHost().getDnsName();
                }
                String urlString;
                if (argslist != null) {
                    try {
                        urlString = String.format(scheme + "://" + urlhost + portString + file, argslist.toArray());
                        urlString = Util.parseTemplate(urlString, p.getHost(), argslist, p);
                    } catch (IllegalFormatConversionException e) {
                        throw new MalformedURLException(String.format("Illegal format string: %s://%s:%s%s, args %d", scheme, urlhost, portString, file, argslist.size()));
                    }
                } else {
                    urlString = Util.parseTemplate(scheme + "://" + urlhost + portString + file, p, p.getHost());
                }
                generatedUrl = new URL(urlString);
            } else {
                generatedUrl = url;
            }
            return generatedUrl;
        }
    }
    public static UrlBuilder builder() {
        return new UrlBuilder();
    }

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
        builder.evictIdleConnections(step, TimeUnit.SECONDS);
        try {
            builder.setSSLContext(getSSLSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            log(Level.ERROR, "No default SSLContext available", e);
        }
        builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timeout * 1000).setTcpNoDelay(true).build());
        builder.setConnectionManagerShared(false);
        builder.setMaxConnPerRoute(2);
        builder.setMaxConnTotal(maxConnect * 2);

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
            log(Level.ERROR, "http client closed failed: %s", e);
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
