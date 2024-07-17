package jrds.probe;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.Optional;
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
import jrds.starter.Connection;
import jrds.starter.SSLStarter;
import jrds.starter.Starter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

public class HttpClientStarter extends Starter {

    @Setter
    @Accessors(chain = true) @ToString
    public static class UrlBuilder {
        private URL url = null;
        private String urlhost = null;
        private int port = -1;
        private String file = "/";
        String scheme = null;
        private String login = null;
        private String password = null;
        private Probe<?, ?> probe = null;
        private Connection<?> connection = null;

        private UrlBuilder() {}

        public URL build() throws MalformedURLException {
            return build(List.of());
        }

        public URL build(List<Object> argslist) throws MalformedURLException {
            if (argslist == null) {
                argslist = List.of();
            }
            URL generatedUrl;
            if (url == null) {
                if (port <= 0 && (scheme == null || scheme.isEmpty())) {
                    scheme = "http";
                } else if (scheme == null || scheme.isEmpty()) {
                    if (port == 443) {
                        scheme = "https";
                    } else {
                        scheme = "http";
                    }
                }
                String portString = port < 0 ? "" : (":" + port);
                if (urlhost == null && probe != null) {
                    urlhost = probe.getHost().getDnsName();
                }
                if (urlhost == null && connection != null) {
                    urlhost = connection.getHostName();
                }
                List<Object> templateArgs = new ArrayList<>();
                templateArgs.add(argslist);
                Optional.ofNullable(probe).ifPresent(p -> {
                    templateArgs.add(p);
                    templateArgs.add(p.getHost());
                });
                Optional.ofNullable(connection).ifPresent(templateArgs::add);
                try {
                    String urlString = String.format(scheme + "://" + urlhost + portString + file, argslist.toArray());
                    urlString = Util.parseTemplate(urlString, templateArgs.toArray());
                    generatedUrl = new URL(urlString);
                } catch (IllegalFormatConversionException e) {
                    throw new MalformedURLException(String.format("Illegal format string: %s://%s:%s%s, args %d", scheme, urlhost, portString, file, argslist.size()));
                }
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
        return client != null && (sslfactory == null || sslfactory.isStarted());
    }

}
