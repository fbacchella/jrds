package jrds.probe.jmx;

import java.util.Collection;

import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pConnectException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.exception.J4pTimeoutException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;
import org.slf4j.event.Level;

import jrds.InstanceLogger;

public class JolokiaJmxSource extends JmxAbstractDataSource<J4pClient> {

    JolokiaJmxSource(J4pClient connection) {
        super(connection);
    }

    @Override
    public Number getValue(InstanceLogger node, RequestParams params) {
        J4pReadRequest req = new J4pReadRequest(params.mbeanName, params.attributeName);
        try {
            J4pReadResponse resp = connection.execute(req);
            Object o = resp.getValue();
            return resolvJmxObject(o, params);
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
        if (ex instanceof J4pRemoteException) {
            J4pRemoteException rex = (J4pRemoteException) ex;
            String remoteStack;
            if (node.getInstanceLogger().isDebugEnabled() && rex.getRemoteStackTrace() != null) {
                remoteStack = "\nError stack:\n" + rex.getRemoteStackTrace();
            } else {
                remoteStack = "";
            }
            node.log(Level.ERROR, "Remote Jolokia exception: %s" + remoteStack, rex.getMessage());
        } else {
            String message = null;
            if (ex instanceof J4pConnectException || ex instanceof J4pTimeoutException) {
                message = "Jolokia IO exception: %s";
            } else {
                message = "Jolokia local exception: %s";
            }
            node.log(Level.ERROR, ex, message, ex.getMessage());
        }
    }

}
