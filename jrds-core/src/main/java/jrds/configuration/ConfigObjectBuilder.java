package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rrd4j.DsType;

import jrds.ProbeDesc;
import jrds.ProbeDesc.DataSourceBuilder;
import jrds.PropertiesManager;
import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.webapp.RolesACL;
import jrds.webapp.WithACL;

abstract class ConfigObjectBuilder<BuildObject> {
    static final private Logger logger = LoggerFactory.getLogger(ConfigObjectBuilder.class);

    PropertiesManager pm;
    public ConfigType ct;

    abstract BuildObject build(JrdsDocument n) throws InvocationTargetException;

    protected ConfigObjectBuilder(ConfigType ct) {
        this.ct = ct;
    }

    /**
     * Add a roles ACL to the object being build, but only if security was set
     * in the properties. If the xpath match no roles, the object will have no
     * ACL set, so it will use it's own default ACL.
     * 
     * @param object The object to add a role to
     * @param n The DOM tree where the xpath will look into
     * @param roleElements the role element
     */
    protected void doACL(WithACL object, JrdsDocument n, JrdsElement roleElements) {
        if(pm.security) {
            List<String> roles = new ArrayList<String>();
            for(JrdsElement e: roleElements.getChildElementsByName("role")) {
                roles.add(e.getTextContent());
            }
            if(roles.size() > 0) {
                object.addACL(new RolesACL(new HashSet<String>(roles)));
                object.addACL(pm.adminACL);
            } else {
                object.addACL(pm.defaultACL);
            }
        }
    }

    /**
     * Extract the data store list from a DOM node, it must contains a list of
     * ds elements
     * 
     * @param name the name of the graph desc being build
     * @param node a DOM node wrapped in a JrdsNode
     * @return a list of Map describing the data sources
     */
    protected List<DataSourceBuilder> doDsList(String name, JrdsElement node) {
        if(node == null) {
            return Collections.emptyList();
        }
        List<DataSourceBuilder> dsList = new ArrayList<>();
        for(JrdsElement dsNode: node.getChildElementsByName("ds")) {
            DataSourceBuilder builder = ProbeDesc.getDataSourceBuilder();
            for(JrdsElement dsContent: dsNode.getChildElements()) {
                String textValue = dsContent.getTextContent().trim();
                String nodeName = dsContent.getNodeName();
                if (nodeName.startsWith("collect") || nodeName.startsWith("oid")) {
                    builder.setOptionnal(Boolean.valueOf(dsContent.getAttribute("optional")));
                }
                switch (nodeName) {
                case "dsName":
                    builder.setName(textValue);
                    break;
                case "dsType":
                    if(!"none".equalsIgnoreCase(textValue)) {
                        try {
                            builder.setDsType(DsType.valueOf(textValue.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid ds type specified for " + name + ": " + textValue);
                            builder = null;
                        }
                    }
                    break;
                case "collect":
                case "oid":
                    builder.setCollectKey(textValue);
                    break;
                case "collecthigh":
                case "oidhigh":
                    builder.setCollectKeyHigh(textValue);
                    break;
                case "collectlow":
                case "oidlow":
                    builder.setCollectKeyLow(textValue);
                    break;
                case "defaultValue":
                    builder.setDefaultValue(Util.parseStringNumber(textValue, Double.NaN));
                    break;
                case "minValue":
                    builder.setMinValue(Util.parseStringNumber(textValue, 0.0));
                    break;
                case "maxValue":
                    builder.setMaxValue(Util.parseStringNumber(textValue, Double.NaN));
                    break;
                }
                if (builder == null) {
                    break;
                }
            }
            if (builder != null) {
                dsList.add(builder);
            }
        }
        logger.trace("data store list build: {}", dsList);
        return dsList;
    }

    /**
     * @param pm the pm to set
     */
    void setPm(PropertiesManager pm) {
        this.pm = pm;
    }

    /**
     * Apply a method on a object with the value found in the XML element If the
     * element is null, the method does nothing.
     * 
     * @param e
     * @param o
     * @param method
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public boolean setMethod(JrdsElement e, Object o, String method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return setMethod(e, o, method, String.class);
    }

    /**
     * Apply a method on a object with the value found in a collection of XML
     * elements If the element is null, the method does nothing.
     * 
     * @param e
     * @param o
     * @param method
     * @param argType
     * @return true if a least one set method succed
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public boolean setMethod(Iterable<JrdsElement> e, Object o, String method, Class<?> argType) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(e == null)
            return false;
        boolean setted = false;
        for(JrdsElement elem: e) {
            setted |= setMethod(elem, o, method, argType);
        }
        return setted;
    }

    /**
     * Apply a method on a object with the value found in the XML element.
     * <p>
     * If the element is null, the method does nothing.
     * <p>
     * The text value of the element is parsed to the type given in the argument
     * argType. This type must have a constructor that take a String argument.
     * 
     * @param element
     * @param o
     * @param method
     * @param argType
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public boolean setMethod(JrdsElement element, Object o, String method, Class<?> argType) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(element == null)
            return false;

        String name = element.getTextContent();
        if(name == null)
            return false;

        Method m;
        try {
            m = o.getClass().getMethod(method, argType);
        } catch (NoSuchMethodException e) {
            m = o.getClass().getMethod(method, Object.class);
        }
        m.invoke(o, ArgFactory.ConstructFromString(argType, name.trim()));
        return true;
    }

}
