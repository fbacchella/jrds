package jrds.probe.jmx;

import java.util.Collection;
import java.util.Optional;

import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pConnectException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.exception.J4pTimeoutException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;
import org.slf4j.event.Level;

import jrds.InstanceLogger;

public class JolokiaJmxSource extends JmxAbstractDataSource<J4pClient> {

    private final JolokiaJmxConnection j4pConnection;

    JolokiaJmxSource(JolokiaJmxConnection j4pConnection, J4pClient connection) {
        super(connection);
        this.j4pConnection = j4pConnection;
    }

    @Override
    public Number getValue(InstanceLogger node, RequestParams params) {
        J4pReadRequest req = j4pConnection.readRequest(params);
        return j4pConnection.getValue(req)
                            .or(() -> Optional.ofNullable(executeRead(node, req)))
                            .map(J4pResponse::getValue)
                            .map(o -> resolvJmxObject(o, params))
                            .orElse(null);
    }

    private J4pReadResponse executeRead(InstanceLogger node, J4pReadRequest req) {
        try {
            return connection.execute(req);
        } catch (J4pException ex) {
            handleJolokiaException(node, ex);
            return null;
        }
    }

    @Override
    public Collection<ObjectName> getNames(InstanceLogger node, ObjectName dest) {
        try {
            J4pSearchRequest req = new J4pSearchRequest(AbstractJmxConnection.startTimeRequestsParams.mbeanName);
            J4pSearchResponse resp = connection.execute(req);
            return resp.getObjectNames();
        } catch (J4pException ex) {
            handleJolokiaException(node, ex);
            return null;
        }
    }

    private void handleJolokiaException(InstanceLogger node, J4pException ex) {
        if (ex instanceof J4pRemoteException rex) {
            String remoteStack;
            if (node.getInstanceLogger().isDebugEnabled() && rex.getRemoteStackTrace() != null) {
                remoteStack = "\nError stack:\n" + rex.getRemoteStackTrace();
            } else {
                remoteStack = "";
            }
            node.log(Level.ERROR, "Remote Jolokia exception: %s" + remoteStack, rex.getMessage());
        } else {
            String message;
            if (ex instanceof J4pConnectException || ex instanceof J4pTimeoutException) {
                message = "Jolokia IO exception: %s";
            } else {
                message = "Jolokia local exception: %s";
            }
            node.log(Level.ERROR, ex, message, ex.getMessage());
        }
    }

}
