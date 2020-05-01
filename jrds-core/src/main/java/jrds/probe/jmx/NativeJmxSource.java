package jrds.probe.jmx;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.event.Level;

import jrds.InstanceLogger;
import jrds.Probe;
import jrds.Util;

public class NativeJmxSource extends JmxAbstractDataSource<MBeanServerConnection>{

    public NativeJmxSource(MBeanServerConnection connection) {
        super(connection);
    }

    @Override
    public Number getValue(InstanceLogger node, RequestParams params) {
        Number v = null;
        try {
            Object attr = connection.getAttribute(params.mbeanName, params.attributeName);
            v = resolvJmxObject(attr, params);
        } catch (AttributeNotFoundException ex) {
            node.log(Level.ERROR, ex, "Invalid JMX attribute %s", params.attributeName);
        } catch (InstanceNotFoundException ex) {
            Level l = Level.ERROR;
            if (node instanceof Probe) {
                @SuppressWarnings("unchecked")
                Probe<String, Double> p = (Probe<String, Double>) node;
                if (p.isOptional(params.jmxCollectPath)) {
                    l = Level.DEBUG;
                }
           }
           node.log(l, "JMX instance not found: %s", ex);
        } catch (MBeanException ex) {
            Throwable cause = ex.getCause();
            String format = "JMX MBeanException: %s";
            if (cause instanceof RemoteException) {
                cause = cause.getCause();
                format = "Remote RMI exception: %s";
            }
            node.log(Level.ERROR, cause, format, Util.resolveThrowableException(cause));
        } catch (ReflectionException ex) {
            node.log(Level.ERROR, ex, "JMX reflection error: %s", ex);
        } catch (IOException ex) {
            node. log(Level.ERROR, ex, "JMX IO error: %s", ex);
        }
        return v;
    }

    @Override
    public Set<ObjectName> getNames(InstanceLogger node, ObjectName dest) {
        try {
            return connection.queryNames(dest, null);
        } catch (IOException ex) {
            node. log(Level.ERROR, ex, "JMX IO error: %s", ex);
            return Collections.emptySet();
        }
    }

}
