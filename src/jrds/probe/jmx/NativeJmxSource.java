package jrds.probe.jmx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

public class NativeJmxSource extends JmxAbstractDataSource<MBeanServerConnection>{

    public NativeJmxSource(MBeanServerConnection connection) {
        super(connection);
    }

    @Override
    public Number getValue(ObjectName mbeanName, String attributeName, String[] jmxPath) throws InvocationTargetException {
        try {
            Object attr = connection.getAttribute(mbeanName, attributeName);
            Number v = resolvJmxObject(attr, jmxPath);
            return v;
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
            throw new InvocationTargetException(e);
        }
    }

    /**
     * Try to extract a numerical value from a jmx Path pointing to a jmx object
     * If the attribute (element 0) of the path is a :
     * - Set, array or TabularData, the size is used
     * - Map, the second element is the key to the value
     * - CompositeData, the second element is the key to the value
     * @param jmxPath
     * @param o
     * @return
     * @throws UnsupportedEncodingException
     */
    Number resolvJmxObject(Object o, String[] jmxPath) throws UnsupportedEncodingException {
        Object value;
        // Fast simple case
        if(o instanceof Number)
            return (Number) o;
        else if(o instanceof CompositeData && jmxPath.length == 2) {
            String subKey = URLDecoder.decode(jmxPath[1], "UTF-8");
            CompositeData co = (CompositeData) o;
            value = co.get(subKey);
        } else if(o instanceof Map<?, ?> && jmxPath.length == 2) {
            String subKey = URLDecoder.decode(jmxPath[1], "UTF-8");
            value = ((Map<?, ?>) o).get(subKey);
        } else if(o instanceof Collection<?>) {
            return ((Collection<?>) o).size();
        } else if(o instanceof TabularData) {
            return ((TabularData) o).size();
        } else if(o.getClass().isArray()) {
            return Array.getLength(o);
        }
        // Last try, make a wild guess
        else {
            value = o;
        }
        if(value instanceof Number) {
            return ((Number) value);
        } else if(value instanceof String) {
            return jrds.Util.parseStringNumber((String) value, Double.NaN);
        }
        return Double.NaN;
    }

}
