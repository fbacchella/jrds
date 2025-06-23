package jrds.probe.jmx;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pBulkRemoteException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.exception.J4pTimeoutException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pVersionRequest;
import org.jolokia.json.JSONObject;
import org.slf4j.event.Level;

import jrds.PropertiesManager;
import jrds.probe.HttpClientStarter;
import jrds.probe.JMXConnection;
import lombok.Data;

public class JolokiaJmxConnection extends AbstractJmxConnection<J4pClient, JolokiaJmxSource> {

    @Data
    private static class ReadRequestKey {
        private final ObjectName mbeanName;
        private final String attributeName;
        ReadRequestKey(RequestParams params) {
            this.mbeanName = params.mbeanName;
            this.attributeName = params.attributeName;
        }
    }

    private J4pClient j4pClient;
    private JolokiaJmxSource connection;

    private final Map<ReadRequestKey, J4pReadRequest> j4pRequests = new HashMap<>();
    private Map<J4pReadRequest, J4pReadResponse> mapping = Map.of();

    public JolokiaJmxConnection(JMXConnection parent) {
        super(parent);
        path = "/jolokia/";
        port = -1;
    }

    @Override
    public void configure(PropertiesManager pm) {
        getLevel().getParent().registerStarter(
                HttpClientStarter.class, HttpClientStarter.class.getName(), HttpClientStarter::new
        );
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
        int resolvedPort = port > 0 ? port : ssl ? 443 : 80;
        URL url;
        try {
            url = new URI(protocol, null, getHostName(), resolvedPort, path, null, null).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            log(Level.ERROR, e, "can't build jolokia URL: %s", e.getMessage());
            return false;
        }
        try {
            j4pClient = new J4pClient(url.toString(), httpstarter.getHttpClient());
            J4pResponse<J4pVersionRequest> ver = j4pClient.execute(new J4pVersionRequest());
            if (ver.getValue() != null) {
                if (! j4pRequests.isEmpty()) {
                    doValueCache();
                }
                /*List<J4pReadRequest> requests = j4pRequests.values().stream().toList();
                log(Level.DEBUG, "Will batch %d requests", requests.size());
                System.err.format("Will batch %d requests%n", requests.size());
                List<J4pReadResponse> responses = j4pClient.execute(requests);
                mapping = IntStream.range(0, requests.size())
                                   .mapToObj(i -> Map.entry(requests.get(i), responses.get(i)))
                                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));*/
                connection = new JolokiaJmxSource(this, j4pClient);
                return true;
            } else {
                return false;
            }
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

    private void doValueCache() throws J4pException{
        List<J4pReadRequest> requests = j4pRequests.values().stream().toList();
        List<J4pReadResponse> tryResponses;
        try {
            log(Level.DEBUG, "Will batch %d requests", requests.size());
            tryResponses = j4pClient.execute(requests);
        } catch (J4pBulkRemoteException e) {
            tryResponses = e.getResults()
                            .stream()
                            .map(o -> o instanceof J4pReadResponse ? (J4pReadResponse) o : null)
                            .collect(Collectors.toList());
        } catch (J4pException ex) {
            throw ex;
        }
        List<J4pReadResponse> responses = tryResponses;
        mapping = IntStream.range(0, requests.size())
                           .filter(i -> responses.get(i) != null)
                           .mapToObj(i -> Map.entry(requests.get(i), responses.get(i)))
                           .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    @Override
    public void stopConnection() {
        j4pClient = null;
        connection = null;
        mapping = Map.of();
    }

    @Override
    public long setUptime() {
        return Optional.ofNullable(connection.getValue(this.getLevel(), startTimeRequestsParams)).orElse(0).longValue();
    }

    @Override
    public <T> T getMBean(String name, Class<T> interfaceClass) {
        throw new UnsupportedOperationException("Can't get generic mbean class from jolokia");
    }

    public J4pReadRequest readRequest(RequestParams params) {
        return j4pRequests.computeIfAbsent(new ReadRequestKey(params), p -> new J4pReadRequest(p.mbeanName, p.attributeName));
    }

    public Optional<J4pReadResponse> getValue(J4pReadRequest request) {
        return Optional.ofNullable(mapping.get(request));
    }

}
