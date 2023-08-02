package jrds.factories;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GenericBean;
import jrds.factories.xml.JrdsElement;

/**
 * A class to build args from a string constructor
 * 
 * @author Fabrice Bacchella
 */

public final class ArgFactory {
    static private final Logger logger = LoggerFactory.getLogger(ArgFactory.class);

    static private final String[] argPackages = new String[] { "java.lang.", "java.net.", "org.snmp4j.smi.", "java.io.", "" };
    static private final Map<String, Class<?>> classCache = new ConcurrentHashMap<String, Class<?>>();

    /**
     * This method build a list from an XML enumeration of element.
     * 
     * The enumeration is made of :
     * <p>
     * <code>&lt;arg type="type" value="value"&gt;</code>
     * <p>
     * or
     * <p>
     * <code>&lt;arg type="type"&gt;value&lt;/value&gt;</code>
     * <p>
     * This method is recursive, so it if finds some <code>list</code> elements
     * instead of an <code>arg</code>, it will build a sub-list.
     * 
     * Unknown element will be silently ignored.
     * 
     * @param sequence an XML element that contains as sequence of
     *            <code>arg</code> or <code>list</code> elements.
     * @param arguments some object that will be used by a call to
     *            <code>jrds.Util.parseTemplate</code> for the arg values
     * @return
     * @throws InvocationTargetException
     */
    public static List<Object> makeArgs(JrdsElement sequence, Object... arguments) throws InvocationTargetException {
        List<JrdsElement> elements = sequence.getChildElements();
        List<Object> argsList = new ArrayList<Object>(elements.size());
        for(JrdsElement listNode: elements) {
            String localName = listNode.getNodeName();
            logger.trace("Element to check: {}", localName);
            if("arg".equals(localName)) {
                String type = listNode.getAttribute("type");
                String value;
                if(listNode.hasAttribute("value"))
                    value = listNode.getAttribute("value");
                else
                    value = listNode.getTextContent();
                value = jrds.Util.parseTemplate(value, arguments);
                Object o = ArgFactory.ConstructFromString(resolvClass(type), value);
                argsList.add(o);
            } else if("list".equals(localName)) {
                argsList.add(makeArgs(listNode, arguments));
            }
        }
        logger.debug("arg vector: {}", argsList);
        return argsList;
    }

    static Class<?> resolvClass(String name) {
        if(classCache.containsKey(name))
            return classCache.get(name);
        Class<?> retValue = null;
        if("int".equals(name))
            return Integer.TYPE;
        else if("double".equals(name)) {
            return Double.TYPE;
        } else if("float".equals(name)) {
            return Float.TYPE;
        } else if("byte".equals(name)) {
            return Byte.TYPE;
        } else if("long".equals(name)) {
            return Long.TYPE;
        } else if("short".equals(name)) {
            return Short.TYPE;
        } else if("boolean".equals(name)) {
            return Boolean.TYPE;
        } else if("char".equals(name)) {
            return Character.TYPE;
        }
        for(String packageTry: argPackages) {
            try {
                retValue = Class.forName(packageTry + name);
            } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            }
        }
        if(retValue == null)
            throw new RuntimeException("Class " + name + " not found");
        classCache.put(name, retValue);
        return retValue;
    }

    /**
     * Create an object providing the class and a String argument. So the class must have
     * a constructor taking only a string as an argument.
     * 
     * It can manage native type and return an boxed object
     * @param <T> The value type
     * @param clazz the class of the new object
     * @param value the value as a string
     * @return a conversion from String
     * @throws InvocationTargetException if it fails to construct the value
     */
    @SuppressWarnings("unchecked")
    public static <T> T ConstructFromString(Class<T> clazz, String value) throws InvocationTargetException {
        try {
            Constructor<T> c = null;
            if (clazz == Integer.TYPE || Integer.class.equals(clazz)) {
                return (T) Integer.valueOf(value);
            } else if (clazz == Double.TYPE || Integer.class.equals(clazz)) {
                return (T) Double.valueOf(value);
            } else if (clazz == Float.TYPE || Float.class.equals(clazz)) {
                return (T) Float.valueOf(value);
            } else if (clazz == Byte.TYPE || Byte.class.equals(clazz)) {
                return (T) Byte.valueOf(value);
            } else if (clazz == Long.TYPE || Long.class.equals(clazz)) {
                return (T) Long.valueOf(value);
            } else if (clazz == Short.TYPE || Short.class.equals(clazz)) {
                return (T) Short.valueOf(value);
            } else if (clazz == Boolean.TYPE || Boolean.class.equals(clazz)) {
                c = (Constructor<T>) Boolean.class.getConstructor(String.class);
            } else if (clazz == Character.TYPE || Character.class.equals(clazz)) {
                c = (Constructor<T>) Character.class.getConstructor(String.class);
            } else {
                c = clazz.getConstructor(String.class);
            }
            return c.newInstance(value);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (InstantiationException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e, clazz.getName());
        } catch (InvocationTargetException e) {
            throw e;
        }
    }

    /**
     * Given an object, a bean name and a bean value, try to set the bean.
     * 
     * The bean type is expect to have a constructor taking a String argument
     * 
     * @param o the object to set
     * @param beanName the bean to set
     * @param beanValue the bean value
     * @throws InvocationTargetException
     */
    static public void beanSetter(Object o, String beanName, String beanValue) throws InvocationTargetException {
        try {
            PropertyDescriptor bean = new PropertyDescriptor(beanName, o.getClass());
            Method setMethod = bean.getWriteMethod();
            if(setMethod == null) {
                throw new InvocationTargetException(new NullPointerException(), String.format("Unknown bean %s", beanName));
            }
            Class<?> setArgType = bean.getPropertyType();
            Object argInstance = ArgFactory.ConstructFromString(setArgType, beanValue);
            setMethod.invoke(o, argInstance);
        } catch (InvocationTargetException e) {
            throw new InvocationTargetException(e.getCause(), "invalid bean '" + beanName + "' for " + o);
        } catch (Exception e) {
            throw new InvocationTargetException(e, "invalid bean '" + beanName + "' for " + o);
        }
    }

    /**
     * Extract a map of the beans of an class. Only the beans listed in the
     * ProbeBean class will be return
     * 
     * @param c a class to extract beans from
     * @return
     * @throws InvocationTargetException
     */
    static public Map<String, GenericBean> getBeanPropertiesMap(Class<?> c, Class<?> topClass) throws InvocationTargetException {
        Set<ProbeBean> beansAnnotations = ArgFactory.enumerateAnnotation(c, ProbeBean.class, topClass);
        if(beansAnnotations.isEmpty())
            return Collections.emptyMap();
        Map<String, GenericBean> beanProperties = new HashMap<String, GenericBean>();
        for(ProbeBean annotation: beansAnnotations) {
            for(String beanName: annotation.value()) {
                // Bean already found, don't work on it again
                if(beanProperties.containsKey(beanName)) {
                    continue;
                }
                try {
                    PropertyDescriptor bean = new PropertyDescriptor(beanName, c);
                    beanProperties.put(bean.getName(), new GenericBean.JavaBean(bean));
                } catch (IntrospectionException e) {
                    throw new InvocationTargetException(e, "invalid bean " + beanName + " for " + c.getName());
                }

            }
        }
        return beanProperties;
    }

    /**
     * Enumerate the hierarchy of annotation for a class, until a certain class
     * type is reached
     * 
     * @param searched the Class where the annotation is searched
     * @param annontationClass the annotation class
     * @param stop a class that will stop (included) the search
     * @return
     */
    static public <T extends Annotation> Set<T> enumerateAnnotation(Class<?> searched, Class<T> annontationClass, Class<?> stop) {
        Set<T> annotations = new LinkedHashSet<T>();
        while (searched != null && stop.isAssignableFrom(searched)) {
            if(searched.isAnnotationPresent(annontationClass)) {
                T annotation = searched.getAnnotation(annontationClass);
                annotations.add(annotation);
            }
            for(Class<?> i: searched.getInterfaces()) {
                if(i.isAnnotationPresent(annontationClass)) {
                    T annotation = i.getAnnotation(annontationClass);
                    annotations.add(annotation);
                }
            }
            searched = searched.getSuperclass();
        }
        return annotations;
    }

}
