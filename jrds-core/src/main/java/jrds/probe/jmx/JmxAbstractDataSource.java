package jrds.probe.jmx;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import jrds.InstanceLogger;

public abstract class JmxAbstractDataSource<CNX> {

    public final CNX connection;

    JmxAbstractDataSource(CNX connection) {
        this.connection = connection;
    }

    protected abstract Number getValue(InstanceLogger node, RequestParams params);
    
    public abstract Collection<ObjectName> getNames(InstanceLogger node, ObjectName dest);

    public Number collect(InstanceLogger node, String jmxCollectPath) throws MalformedObjectNameException {
        RequestParams params = new RequestParams(jmxCollectPath);
        return getValue(node, params);
    }

    /**
     * Try to extract a numerical value from a jmx Path pointing to a jmx object
     * If the attribute (element 0) of the path is a :
     * - Set, array or TabularData, the size is used
     * - Map, the second element is the key to the value
     * - CompositeData, the second element is the key to the value
     * @param o
     * @param params
     * @return
     */
    protected Number resolvJmxObject(Object o, RequestParams params) {
        // Fast simple case
        if (o instanceof Number) {
            return (Number) o;
        } else if (o instanceof CompositeData && params != null) {
            CompositeData co = (CompositeData) o;
            return resolvJmxObject(co.get(params.jmxPath), null);
        } else if (o instanceof Map<?, ?> && params.jmxPath != null) {
            return resolvJmxObject(((Map<?, ?>) o).get(params.jmxPath), null);
        } else if (o instanceof Collection<?>) {
            return ((Collection<?>) o).size();
        } else if (o instanceof TabularData) {
            return ((TabularData) o).size();
        } else if (o.getClass().isArray()) {
            return Array.getLength(o);
        } else if (o instanceof String) {
            return jrds.Util.parseStringNumber((String) o, Double.NaN);
        } else {
            return null;
        }
    }

}
