package jrds.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jrds.PropertiesManager;
import jrds.Util;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.webapp.RolesACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.snmp4j.smi.OID;

abstract class ConfigObjectBuilder<BuildObject> {
    static final private Logger logger = Logger.getLogger(ConfigObjectBuilder.class);

    PropertiesManager pm;
    public ConfigType ct;

    abstract BuildObject build(JrdsDocument n) throws InvocationTargetException;

    public ConfigObjectBuilder(ConfigType ct) {
        this.ct = ct;
    }

    public Map<String, String> makeProperties(JrdsElement n) {
        if(n == null)
            return Collections.emptyMap();
        Map<String, String> props = new HashMap<String, String>();
        for(JrdsElement e: n.getChildElementsByName("properties")) {
            JrdsElement propNode = e.getElementbyName("entry");
            String key = propNode.getAttribute("key");
            if(key != null) {
                String value = propNode.getTextContent();
                logger.trace(Util.delayedFormatString("Adding propertie %s=%s", key, value));
                props.put(key, value);
            }
        }
        logger.debug(Util.delayedFormatString("Properties map: %s", props));
        return props;
    }

    /**
     * Add a roles ACL to the object being build, but only if security was set in the properties.
     * If the xpath match no roles, the object will have no ACL set, so it will use it's own default ACL.
     * 
     * @param object The object to add a role to
     * @param n  The DOM tree where the xpath will look into
     * @param xpath where to found the roles
     */
    protected void doACL(WithACL object, JrdsDocument n, List<JrdsElement> roleElements) {
        if(pm.security){
            List<String> roles = new ArrayList<String>(roleElements.size());
            for(JrdsElement e: roleElements) {
                roles.add(e.getTextContent());
            }
            if(roles.size() > 0) {				
                object.addACL(new RolesACL(new HashSet<String>(roles)));
                object.addACL(pm.adminACL);
            }
            else {
                object.addACL(pm.defaultACL);
            }
        }
    }

    /**
     * Extract the data store list from a DOM node, it must contains a list of ds elements
     * @param name the name of the graph desc being build
     * @param node a DOM node wrapped in a JrdsNode
     * @return a list of Map describing the data sources
     */
    protected List<Map<String, Object>> doDsList(String name, JrdsElement node) {
        if(node == null)
            return Collections.emptyList();
        List<Map<String, Object>> dsList = new ArrayList<Map<String, Object>>();
        for(JrdsElement dsNode: node.getChildElementsByName("ds")) {
            Map<String, Object> dsMap = new HashMap<String, Object>(4);
            for(JrdsElement dsContent: dsNode.getChildElements()) {
                String element = dsContent.getNodeName();
                String textValue = dsContent.getTextContent().trim();
                Object value = textValue;
                if( element.startsWith("collect")) {
                    if("".equals(value))
                        value = null;
                }
                else if("dsType".equals(element)) {
                    if( !"NONE".equals(textValue.toUpperCase()))
                        try {
                            value = DsType.valueOf(textValue.toUpperCase());
                        } catch (Exception e) {
                            logger.error("Invalid ds type specified for " + name + ": " + textValue);
                            dsMap = null;
                            break;
                        }
                    else
                        value = null;
                }
                else if(element.startsWith("oid")) {
                    value = new OID(textValue);
                    element = element.replace("oid", "collect");
                }
                dsMap.put(element, value);
            }
            if(dsMap != null)
                dsList.add(dsMap);
        }
        logger.trace(jrds.Util.delayedFormatString("data store list build: %s", dsList));
        return dsList;
    }

    /**
     * @param pm the pm to set
     */
    void setPm(PropertiesManager pm) {
        this.pm = pm;
    }

    public boolean setMethod(JrdsElement parent, Object o, String method, String...path) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (parent == null)
            return false;
        JrdsElement current = parent;
        for(String tag: path) {
            current = current.getElementbyName(tag);
            if(current == null)
                return false;
        }
        return setMethod(current, o, method);
    }

    public boolean setMethod(JrdsElement e, Object o, String method) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return setMethod(e, o, method, String.class);
    }

    public boolean setMethod(JrdsElement element, Object o, String method, Class<?> argType) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException{
        if(element == null)
            return false;
        Constructor<?> c = null;
        if(! argType.isPrimitive() ) {
            c = argType.getConstructor(String.class);
        }
        else if(argType == Integer.TYPE) {
            c = Integer.class.getConstructor(String.class);
        }
        else if(argType == Double.TYPE) {
            c = Double.class.getConstructor(String.class);
        }
        else if(argType == Float.TYPE) {
            c = Float.class.getConstructor(String.class);
        }

        String name = element.getTextContent().trim();
        if(name != null) {
            Method m;
            try {
                m = o.getClass().getMethod(method, argType);
            } catch (NoSuchMethodException e) {
                m = o.getClass().getMethod(method, Object.class);
            }
            m.invoke(o, c.newInstance(name));
            return true;
        }
        return false;
    }
}
