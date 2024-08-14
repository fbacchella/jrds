package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.slf4j.event.Level;

import jrds.ConnectedProbe;
import jrds.factories.ProbeBean;
import jrds.factories.ProbeMeta;
import jrds.starter.Connection;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * A generic probe to collect an HTTP service. It uses
 * <a href="http://hc.apache.org/httpclient-3.x/">Apache's Commons
 * HttpClient</a>s to provide a better isolation with other web app in the same
 * container. So it should used in preference to HttpProbe and deprecate it.
 * default generic : port to provide a default port to collect file to provide a
 * specific file to collect
 * 
 * Implementation should implement the parseStream method
 *
 * @author Fabrice Bacchella
 */
@ProbeMeta(
        timerStarter=jrds.probe.HttpClientStarter.class
        )
@ProbeBean({ "connectionName" })
public abstract class HCHttpProbe<KeyType> extends HttpProbe<KeyType> implements SSLProbe, ConnectedProbe  {

    @Getter @Setter
    protected String connectionName = null;

    private boolean mandatorySession = false;
    // Connection of type HttpSession can be used. It will then hide the HttpClientConnection
    // So store it
    private HttpClientConnection httpcnx;

    @Override
    protected boolean finishConfigure(List<Object> args) {
        httpcnx = registerStarter(
                HttpClientConnection.class,
                Optional.ofNullable(connectionName).orElse(getClass().getName()),
                this::newConnection
        );
        if("true".equalsIgnoreCase(getPd().getSpecific("mandatorySession"))) {
            mandatorySession = true;
        }
        return super.finishConfigure(args);
    }

    private HttpClientConnection newConnection() {
        log(Level.DEBUG, "Instanciating a self registrer HttpClientConnection");
        HttpClientConnection cnx = new HttpClientConnection();
        cnx.setScheme(scheme);
        cnx.setLogin(login);
        cnx.setPassword(password);
        cnx.setPort(port);
        cnx.setFile(file);
        cnx.setName(cnx.getKey().toString());
        return cnx;
    }

    @Override
    protected URL resolveUrl(HttpClientStarter.UrlBuilder builder, List<Object> args) throws MalformedURLException {
        return httpcnx.resolve(builder, this, args);
    }

    @Override
    public Map<KeyType, Number> getNewSampleValues() {
        if (! httpcnx.isStarted()) {
            return Collections.emptyMap();
        }
        HttpClientContext ctx = httpcnx.getClientContext();
        HttpClient client = find(HttpClientStarter.class).getHttpClient();
        HttpEntity entity = null;
        try {
            HttpRequestBase hg = new HttpGet(url.toURI());
            if (!changeRequest(hg)) {
                return Collections.emptyMap();
            }
            URL newUrl = hg.getURI().toURL();
            HttpHost host = new HttpHost(resolver.getInetAddress(), newUrl.getPort(), newUrl.getProtocol());
            HttpResponse response = client.execute(host, hg, ctx);
            if (!validateResponse(response)) {
                EntityUtils.consumeQuietly(response.getEntity());
                return Collections.emptyMap();
            }
            entity = response.getEntity();
            if (entity == null) {
                log(Level.ERROR, "Not response body to %s", getUrl());
                return Collections.emptyMap();
            }
            try (InputStream is = entity.getContent()) {
                Map<KeyType, Number> vars = parseStream(is);
                return vars;
            }
        } catch (HttpHostConnectException e) {
            log(Level.ERROR, e, "Unable to read %s because: %s", getUrl(), e.getCause());
        } catch (IllegalStateException | IOException | URISyntaxException e) {
            log(Level.ERROR, e, "Unable to read %s because: %s", getUrl(), e);
        } finally {
            Optional.ofNullable(entity).ifPresent(EntityUtils::consumeQuietly);
        }

        return Collections.emptyMap();
    }

    /**
     * This method can be overridden to change a request, the default implementation find
     * a defined connection and delegate request changes to it.
     * @param request the request to change
     * @return true if change was successful.
     */
    public boolean changeRequest(HttpRequestBase request) {
        HttpSession session = null;
        if (connectionName != null) {
            log(Level.DEBUG, "looking for session %s", connectionName);
            session = Optional.ofNullable(find(Connection.class, connectionName))
                            .filter(HttpSession.class::isInstance)
                            .map(HttpSession.class::cast)
                            .orElse(null);
            Optional.ofNullable(session).ifPresent(s -> {
                if (!s.makeSession(request)) {
                    log(Level.ERROR, "session failed");
                }
            });
        }
        if (session == null && mandatorySession) {
            log(Level.ERROR, "missing session");
            return false;
        } else {
            return true;
        }
    }

    /**
     * This method can be used to validate the response, the default implementation check
     * that the status code is 200.
     * @param response the responce to check
     * @return true if it's a valid response
     */
    public boolean validateResponse(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() != 200) {
            log(Level.ERROR, "Connection to %s fail with %s", getUrl(), response.getStatusLine().getReasonPhrase());
            return false;
        }
        return true;
    }

}
