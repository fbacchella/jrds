package jrds.probe;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;

import jrds.factories.ProbeMeta;

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
public abstract class HCHttpProbe extends HttpProbe implements SSLProbe {

    private boolean mandatorySession = false;

    @Override
    public Boolean configure() {
        if("true".equalsIgnoreCase(getPd().getSpecific("mandatorySession"))) {
            mandatorySession = true;
        }
        return super.configure();
    }

    @Override
    public Map<String, Number> getNewSampleValues() {
        HttpClientStarter httpstarter = find(HttpClientStarter.class);
        HttpClient cnx = httpstarter.getHttpClient();
        HttpEntity entity = null;
        try {
            HttpGet hg = new HttpGet(getUrl().toURI());
            HttpSession session = null;
            if (connectionName != null) {
               log(Level.DEBUG, "looking for session %s", connectionName);
                session = find(HttpSession.class, connectionName);
                if (session != null) {
                    if (!session.makeSession(hg)) {
                        log(Level.ERROR, "session failed");
                    }
                }
            }
            if (session == null && mandatorySession) {
                log(Level.ERROR, "missing session");
                return null;
            }
            HttpResponse response = cnx.execute(hg);
            if(response.getStatusLine().getStatusCode() != 200) {
                log(Level.ERROR, "Connection to %s fail with %s", getUrl(), response.getStatusLine().getReasonPhrase());
                EntityUtils.consumeQuietly(response.getEntity());
                return null;
            }
            entity = response.getEntity();
            if(entity == null) {
                log(Level.ERROR, "Not response body to %s", getUrl());
                return null;
            }
            InputStream is = entity.getContent();
            Map<String, Number> vars = parseStream(is);
            is.close();
            return vars;
        } catch (IllegalStateException | IOException e) {
            log(Level.ERROR, e, "Unable to read %s because: %s", getUrl(), e.getMessage());
        } catch (URISyntaxException e) {
            log(Level.ERROR, "unable to parse %s", getUrl());
        } finally {
            if(entity != null) {
                EntityUtils.consumeQuietly(entity);
            }
        }

        return null;
    }

}
