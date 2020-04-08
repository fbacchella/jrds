package jrds.probe.jmx;

import java.lang.reflect.InvocationTargetException;

import javax.management.ObjectName;

public abstract class JmxAbstractDataSource<CNX> {

    public final CNX connection;

    JmxAbstractDataSource(CNX connection) {
        this.connection = connection;
    }

    public abstract Number getValue(ObjectName mbeanName, String attributeName, String[] jmxPath) throws InvocationTargetException;

}
