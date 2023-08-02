package jrds.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.event.Level;

import jrds.Probe;
import jrds.factories.ProbeBean;
import jrds.starter.Resolver;
import lombok.Getter;
import lombok.Setter;

/**
 * A generic probe to collect an HTTP service default generic : port to provide
 * a default port to collect file to provide a specific file to collect
 * 
 * Implementation should implement the parseStream method
 *
 * @author Fabrice Bacchella
 */
@ProbeBean({ "port", "file", "url", "urlhost", "scheme", "login", "password" })
public abstract class HttpProbe<KeyType> extends Probe<KeyType, Number> implements UrlProbe {

    @Getter @Setter
    protected URL url = null;
    @Getter @Setter
    protected String urlhost = null;
    @Getter @Setter
    protected Integer port = null;
    @Getter @Setter
    protected String file = null;
    @Getter @Setter
    protected String scheme = null;
    @Getter @Setter
    protected String login = null;
    @Getter @Setter
    protected String password = null;

    protected Resolver resolver = null;

    public Boolean configure(URL url) {
        this.url = url;
        return finishConfigure(null);
    }

    public Boolean configure(Integer port, String file) {
        this.port = port;
        this.file = file;
        return finishConfigure(null);
    }

    public Boolean configure(Integer port) {
        this.port = port;
        return finishConfigure(null);
    }

    public Boolean configure(String file) {
        this.file = file;
        return finishConfigure(null);
    }

    public Boolean configure(List<Object> argslist) {
        return finishConfigure(argslist);
    }

    public Boolean configure(String file, List<Object> argslist) {
        this.file = file;
        return finishConfigure(argslist);
    }

    public Boolean configure(Integer port, List<Object> argslist) {
        this.port = port;
        return finishConfigure(argslist);
    }

    public Boolean configure(URL url, List<Object> argslist) {
        this.url = url;
        return finishConfigure(argslist);
    }

    public Boolean configure(Integer port, String file, List<Object> argslist) {
        this.port = port;
        this.file = file;
        return finishConfigure(argslist);
    }

    public Boolean configure() {
        return finishConfigure(null);
    }

    protected boolean finishConfigure(List<Object> args) {
        try {
            url = resolveUrl(getUrlBuilder(), args);
            log(Level.DEBUG, "Collected URL: %s", url);
        } catch (MalformedURLException e) {
            log(Level.ERROR, e, "failed resolve URL %s", e);
            return false;
        }
        if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
            resolver = (Resolver) registerStarter(new Resolver(url.getHost()));
        }
        log(Level.DEBUG, "URL to collect is %s", url);
        return true;
    }

    private HttpClientStarter.UrlBuilder getUrlBuilder() {
        HttpClientStarter.UrlBuilder urlbuilder = HttpClientStarter.builder();
        Optional.ofNullable(url).ifPresent(urlbuilder::setUrl);
        Optional.ofNullable(scheme).ifPresent(urlbuilder::setScheme);
        Optional.ofNullable(login).ifPresent(urlbuilder::setLogin);
        Optional.ofNullable(password).ifPresent(urlbuilder::setPassword);
        Optional.ofNullable(port).ifPresent(urlbuilder::setPort);
        Optional.ofNullable(file).ifPresent(urlbuilder::setFile);
        return urlbuilder;
    }

    protected URL resolveUrl(HttpClientStarter.UrlBuilder builder, List<Object> args) throws MalformedURLException {
        return builder.build(this, args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#isCollectRunning()
     */
    @Override
    public boolean isCollectRunning() {
        return (resolver == null ? true : resolver.isStarted()) && super.isCollectRunning();
    }

    /**
     * @param stream A stream collected from the http source
     * @return a map of collected value
     */
    protected abstract Map<KeyType, Number> parseStream(InputStream stream);

    /**
     * A utility method that transform the input stream to a List of lines
     * 
     * @param stream
     * @return
     */
    public List<String> parseStreamToLines(InputStream stream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))){
            List<String> lines = new ArrayList<>();
            String lastLine;
            while ((lastLine = in.readLine()) != null)
                lines.add(lastLine);
            return lines;
        } catch (IOException e) {
            log(Level.ERROR, e, "Unable to read url %s because: %s", url, e);
            return Collections.emptyList();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#getNewSampleValues()
     */
    public Map<KeyType, Number> getNewSampleValues() {
        log(Level.DEBUG, "Getting %s", getUrl());
        URLConnection cnx;
        try {
            cnx = getUrl().openConnection();
            cnx.setConnectTimeout(getTimeout() * 1000);
            cnx.setReadTimeout(getTimeout() * 1000);
            cnx.connect();
        } catch (IOException e) {
            log(Level.ERROR, e, "Connection to %s failed: %s", getUrl(), e);
            return null;
        }
        try (InputStream is = cnx.getInputStream()) {
            Map<KeyType, Number> vars = parseStream(is);
            return vars;
        } catch (ConnectException e) {
            log(Level.ERROR, e, "Connection refused to %s", getUrl());
        } catch (IOException e) {
            // Clean http connection error management
            // see
            // http://java.sun.com/j2se/1.5.0/docs/guide/net/http-keepalive.html
            try (InputStream es = ((HttpURLConnection) cnx).getErrorStream()){
                byte[] buffer = new byte[4096];
                int respCode = ((HttpURLConnection) cnx).getResponseCode();
                log(Level.ERROR, e, "Unable to read url %s because: %s, http error code: %d", getUrl(), e, respCode);
                // read the response body
                while (es.read(buffer) > 0) {
                }
            } catch (IOException ex) {
                log(Level.ERROR, ex, "Unable to recover from error in url %s because %s", getUrl(), ex);
            }
        }

        return null;
    }

    /**
     * @return Returns the url.
     */
    public String getUrlAsString() {
        return getUrl().toString();
    }

    @Override
    public String getSourceType() {
        return "HTTP";
    }
}
