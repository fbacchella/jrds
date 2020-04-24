package jrds.probe.jmx;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Map;

import javax.management.ObjectName;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;

public class JolokiaJmxSource extends JmxAbstractDataSource<J4pClient> {

    JolokiaJmxSource(J4pClient connection) {
        super(connection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Number getValue(ObjectName mbeanName, String attributeName, String[] jmxPath) throws InvocationTargetException {
        J4pReadRequest req = new J4pReadRequest(mbeanName, attributeName);
        try {
            J4pReadResponse resp = connection.execute(req);
            Object o = resp.getValue();
            if (jmxPath.length > 1) {
                for (String i: Arrays.copyOfRange(jmxPath, 1, jmxPath.length)) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) o;
                        o = m.get(i);
                        if (o == null) {
                            return null;
                        }
                    }
                }
            }
            if (o instanceof Number) {
                return (Number) o;
            } else if (o instanceof String) {
                try {
                    return Integer.parseInt((String) o);
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
            } else {
                return Double.NaN;
            }
        } catch (J4pRemoteException e) {
            Map<String, Object> errorValue = e.getErrorValue();
            if (errorValue != null) {
                throw new InvocationTargetException(new RemoteException(e.getErrorType() + ": " + errorValue.get("message").toString()));
            } else {
                throw new InvocationTargetException(new IOException("HTTP status: "+ e.getStatus()));
            }
        } catch (J4pException e) {
            if (e.getCause() != null) {
                throw new InvocationTargetException(e.getCause());
            } else {
                throw new InvocationTargetException(e);
            }
        }
    }

}
