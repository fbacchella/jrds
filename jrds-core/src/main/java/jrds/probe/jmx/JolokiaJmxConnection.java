package jrds.probe.jmx;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.exception.J4pTimeoutException;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pVersionRequest;
import org.json.simple.JSONObject;
import org.slf4j.event.Level;

import jrds.PropertiesManager;
import jrds.probe.HttpClientStarter;
import jrds.probe.JMXConnection;

public class JolokiaJmxConnection extends AbstractJmxConnection<J4pClient, JolokiaJmxSource> {

    private J4pClient j4pClient;
    private JolokiaJmxSource connection;

    public JolokiaJmxConnection(JMXConnection parent) {
        super(parent);
        path = "/jolokia/";
        port = -1;
    }

    @Override
    public void configure(PropertiesManager pm) {
        getLevel().getParent().registerStarter(new HttpClientStarter());
        super.configure(pm);
    }

    @Override
    public JolokiaJmxSource getConnection() {
        return connection;
    }

    @Override
    public boolean startConnection() {
        HttpClientStarter httpstarter = getLevel().find(HttpClientStarter.class);
        if (httpstarter == null || !httpstarter.isStarted()) {
            return false;
        }
        String protocol = ssl ? "https" : "http";
        int resolvedport = port > 0 ? port : ssl ? 443 : 80;
        URL url;
        try {
            url = new URL(protocol, getHostName(), resolvedport, path);
        } catch (MalformedURLException e) {
            log(Level.ERROR, e, "can't build jolokia URL: %s", e.getMessage());
            return false;
        }
        try {
            j4pClient = new J4pClient(url.toString(), httpstarter.getHttpClient());
            connection = new JolokiaJmxSource(j4pClient);
            J4pResponse<J4pVersionRequest> ver = j4pClient.execute(new J4pVersionRequest());
            return ver.getValue() != null;
        } catch (J4pRemoteException e) {
            JSONObject errorValue = e.getErrorValue();
            if (errorValue != null) {
                log(Level.ERROR, e, "Can't connect to jolokia URL: %s: %s", e.getErrorType(), errorValue.get("message"));
            } else {
                log(Level.ERROR, e, "Can't connect to jolokia URL: %s (HTTP status %d)", e, e.getStatus());
            }
            return false;
        } catch (J4pTimeoutException e) {
            log(Level.ERROR, "Timeout connecting to jolokia URL: %s", url);
            return false;
        } catch (J4pException e) {
            if (e.getCause() != null) {
                log(Level.ERROR, e, "Can't connect to jolokia URL: %s", e.getCause());
            } else {
                log(Level.ERROR, e, "Can't connect to jolokia URL: %s", e);
            }
            return false;
        }
    }

    @Override
    public void stopConnection() {
        j4pClient = null;
        connection = null;
    }

    @Override
    public long setUptime() {
        return Optional.ofNullable(connection.getValue(this.getLevel(), startTimeRequestsParams)).orElse(0).longValue();
    }

    @Override
    public <T> T getMBean(String name, Class<T> interfaceClass) {
        throw new UnsupportedOperationException("Can't get generic mbean class from jolokia");
    }

}
