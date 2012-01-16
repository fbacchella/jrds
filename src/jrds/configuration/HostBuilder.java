package jrds.configuration;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.ConnectedProbe;
import jrds.HostInfo;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.HostBuilderAgent;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.factories.xml.JrdsNode;
import jrds.starter.ChainedProperties;
import jrds.starter.Connection;
import jrds.starter.Starter;
import jrds.starter.StarterNode;

import org.apache.log4j.Logger;

public class HostBuilder extends ConfigObjectBuilder<HostInfo> {
    static final private Logger logger = Logger.getLogger(HostBuilder.class);

    private ClassLoader classLoader = null;
    private ProbeFactory pf;
    private Map<String, Macro> macrosMap;
    private final Map<Class<?>, Map<String, PropertyDescriptor>> connectionsBeanCache = new HashMap<Class<?>, Map<String, PropertyDescriptor>>();

    public HostBuilder() {
        super(ConfigType.HOSTS);
    }

    @Override
    HostInfo build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeRdsHost(n);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (ClassNotFoundException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        }
    }

    public HostInfo makeRdsHost(JrdsDocument n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        JrdsElement hostNode = n.getRootElement();
        String hostName = hostNode.getAttribute("name");
        String dnsHostname = hostNode.getAttribute("dnsName");
        HostInfo host = null;
        if(hostName == null) {
            return null;
        }
        else if(dnsHostname != null) {
            host = new HostInfo(hostName, dnsHostname);
        }
        else
            host = new HostInfo(hostName);
        host.setHostDir(new File(pm.rrddir, host.getName()));

        String hidden = hostNode.getAttribute("hidden");
        host.setHidden(hidden != null && Boolean.parseBoolean(hidden));

        StarterNode ns = new StarterNode() {};
        Map<String, Set<String>> collections = new HashMap<String, Set<String>>();

        parseFragment(hostNode, host, ns, collections);

        return host;
    }

    private void parseFragment(JrdsElement fragment, HostInfo host, StarterNode ns, Map<String, Set<String>> collections) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        parseSnmp(fragment, host, host);

        makeConnexion(fragment, host);
        for(JrdsElement tagElem: fragment.getChildElementsByName("tag")) {
            try {
                logger.trace(Util.delayedFormatString("adding tag %s to %s", tagElem, host));
                setMethod(tagElem, host, "addTag");
            } catch (InstantiationException e) {
            }
        }

        Map<String, String> hostprop = makeProperties(fragment);
        if(hostprop != null && ! hostprop.isEmpty()) {
            ChainedProperties temp = new ChainedProperties(hostprop);
            ns.registerStarter(temp);
        }

        for(JrdsElement collectionNode: fragment.getChildElementsByName("collection")) {
            String name = collectionNode.getAttribute("name");
            Set<String> set = new HashSet<String>();
            for(JrdsElement e: collectionNode.getChildElementsByName("element")) {
                set.add(e.getTextContent());
            }
            collections.put(name, set);
        }

        for(JrdsElement macroNode: fragment.getChildElementsByName("macro")) {
            String name = macroNode.getAttribute("name");
            Macro m = macrosMap.get(name);
            logger.trace(Util.delayedFormatString("Adding macro %s: %s", name, m));
            if(m != null) {
                Map<String, String> properties = makeProperties(macroNode);
                StarterNode macrosnode = new StarterNode(ns) {};
                ChainedProperties temp = new ChainedProperties(properties);
                macrosnode.registerStarter(temp);

                JrdsDocument hostdoc = (JrdsDocument) fragment.getOwnerDocument();
                //Make a copy of the document fragment
                JrdsNode newDf = JrdsNode.build(hostdoc.importNode(m.getDf(), true));
                JrdsElement macrodef = JrdsNode.build( newDf.getFirstChild());
                parseFragment(macrodef, host, macrosnode, collections);
            }
            else {
                logger.error("Unknown macro:" + name);
            }
        }

        for(JrdsElement forNode: fragment.getChildElementsByName("for")) {
            Map<String, String> forattr = forNode.attrMap();
            String iterprop = forattr.get("var");
            Collection<String> set = null;
            String name = forNode.attrMap().get("collection");
            if(name != null)
                set = collections.get(name);
            else if(forattr.containsKey("min") && forattr.containsKey("max") && forattr.containsKey("step")) {
                int min = Util.parseStringNumber(forattr.get("min"), Integer.MAX_VALUE);
                int max = Util.parseStringNumber(forattr.get("max"), Integer.MIN_VALUE);
                int step = Util.parseStringNumber(forattr.get("step"), Integer.MIN_VALUE);
                if( min > max || step <= 0) {
                    logger.error("invalid range from " + min + " to " + max + " with step " + step);
                    break;
                }
                set = new ArrayList<String>((max - min)/step + 1);
                for(int i=min; i <= max; i+= step) {
                    set.add(Integer.toString(i));
                }
            }

            if(set != null) {
                logger.trace(Util.delayedFormatString("for using %s", set));

                for(String i: set) {
                    Map<String, String> properties = new HashMap<String, String>(1);
                    properties.put(iterprop, i);
                    StarterNode fornode = new StarterNode(ns) {};
                    ChainedProperties temp = new ChainedProperties(properties);
                    fornode.registerStarter(temp);
                    parseFragment(forNode, host, fornode, collections);
                }
            }
            else {
                logger.error("Invalid host configuration, collection " + name + " not found");
            }
        }
        for(JrdsElement probeNode: fragment.getChildElements()) {
            if(! "probe".equals(probeNode.getNodeName()) && ! "rrd".equals(probeNode.getNodeName()) )
                continue;
            try {
                makeProbe(probeNode, host, ns);
            } catch (Exception e) {
                logger.error("Probe creation failed for host " + host.getName() + ": ");
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();  
                e.printStackTrace(new PrintStream(buffer));
                logger.error(buffer);
            }
        }
    }

    public Probe<?,?> makeProbe(JrdsElement probeNode, RdsHost host, StarterNode ns) throws InvocationTargetException {
        Probe<?,?> p = null;
        String type = probeNode.attrMap().get("type");

        List<Map<String, Object>> dsList = doDsList(type, probeNode.getElementbyName("dslist"));
        if(dsList.size() > 0) {
            logger.trace(Util.delayedFormatString("Data source replaced for %s/%s: %s", host, type, dsList));
            ProbeDesc oldpd = pf.getProbeDesc(type);
            try {
                ProbeDesc pd = (ProbeDesc) oldpd.clone();
                pd.replaceDs(dsList);
                List<String> empty = Collections.emptyList();
                pd.setGraphClasses(empty);
                p = pf.makeProbe(pd);
            } catch (CloneNotSupportedException e) {
            }
        }
        else {
            p = pf.makeProbe(type);
        }
        if(p == null)
            return null;
        p.setHost(host);

        ChainedProperties cp = ns.find(ChainedProperties.class);
        String label = probeNode.getAttribute("label");
        if(label != null && ! "".equals(label)) {
            logger.trace(Util.delayedFormatString("Adding label %s to %s", label, p));
            p.setLabel(jrds.Util.parseTemplate(label, cp));;
        }
        if(p instanceof ConnectedProbe) {
            String connexionName = probeNode.getAttribute("connection");
            if(connexionName != null && ! "".equals(connexionName)) {
                logger.trace(Util.delayedFormatString("Adding connection %s to %s", connexionName, p));
                ((ConnectedProbe)p).setConnectionName(jrds.Util.parseTemplate(connexionName, cp));
            }
        }

        ProbeDesc pd = p.getPd();
        List<Object> args = ArgFactory.makeArgs(probeNode, cp, host);
        //Prepare the probe with the default beans values
        Map<String, String> defaultBeans = pd.getDefaultArgs();
        if(defaultBeans!=null) {
            for(Map.Entry<String, String> e: defaultBeans.entrySet()) {
                try {
                    String beanName = e.getKey();
                    String beanValue = e.getValue();
                    PropertyDescriptor bean = pd.getBeanMap().get(beanName);
                    Object value;
                    //If the last argument is a list, give it to the template parser
                    Object lastArgs = args.isEmpty() ? null : args.get(args.size() - 1);
                    if(lastArgs instanceof List) {
                        value = ArgFactory.ConstructFromString(bean.getPropertyType(), Util.parseTemplate(beanValue, host, lastArgs));
                    }
                    else {
                        value = ArgFactory.ConstructFromString(bean.getPropertyType(), jrds.Util.parseTemplate(beanValue, host));
                    }
                    logger.trace(jrds.Util.delayedFormatString("Adding bean %s=%s (%s) to default args", beanName, value, value.getClass()));
                    bean.getWriteMethod().invoke(p, value);
                } catch (Exception ex) {
                    throw new RuntimeException("Invalid default bean " + e.getKey(), ex);
                }
            }
        }

        //Resolve the beans
        try {
            setAttributes(probeNode, p, pd.getBeanMap(), host);
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Can't configure %s for %s: %s", pd.getName(), host, e));
            return null;
        }

        if( !pf.configure(p, args)) {
            logger.error(p + " configuration failed");
            return null;
        }
        if(p != null && p.checkStore()) {
            host.getProbes().add(p);
        }
        else {
            return null;
        }
        parseSnmp(probeNode, p, host);

        Map<String, String> nodeprop = makeProperties(probeNode);
        if(nodeprop != null && nodeprop.size() > 0) {
            ChainedProperties temp = new ChainedProperties(nodeprop);
            p.registerStarter(temp);
        }
        makeConnexion(probeNode, p);
        return p;
    }

    @SuppressWarnings("unchecked")
    private void parseSnmp(JrdsElement node, StarterNode p, HostInfo host) {
        try {
            Class<? extends HostBuilderAgent> c = (Class<? extends HostBuilderAgent>) pm.extensionClassLoader.loadClass("jrds.snmp.SnmpHostBuilderAgent");
            c.getConstructor().newInstance().buildStarters(node, p, host);
        } catch (ClassNotFoundException e) {
            logger.debug("Class jrds.snmp.SnmpHostBuilderAgent not found");
        } catch (InstantiationException e) {
            logger.error("Class jrds.snmp.SnmpHostBuilderAgent can't be instantiated");
        } catch (IllegalArgumentException e) {
        } catch (SecurityException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    private void makeConnexion(JrdsElement domNode, StarterNode sNode) {
        for(JrdsElement cnxNode: domNode.getChildElementsByName("connection")) {
            String type = cnxNode.attrMap().get("type");
            if(type == null) {
                logger.error("No type declared for a connection");
                continue;
            }
            String name = cnxNode.attrMap().get("name");
            Connection<?> o = null;
            try {
                List<Object> args = ArgFactory.makeArgs(cnxNode);
                Class<?> connectionClass = classLoader.loadClass(type);
                Class<?>[] constArgsType = new Class[args.size()];
                Object[] constArgsVal = new Object[args.size()];
                int index = 0;
                for (Object arg: args) {
                    constArgsType[index] = arg.getClass();
                    constArgsVal[index] = arg;
                    index++;
                }
                Constructor<?> theConst = connectionClass.getConstructor(constArgsType);
                o = (Connection<?>)theConst.newInstance(constArgsVal);
                Map<String, PropertyDescriptor> beans = connectionsBeanCache.get(connectionClass);
                if(beans == null) {
                    beans = ArgFactory.getBeanPropertiesMap(connectionClass, Starter.class);
                    connectionsBeanCache.put(connectionClass, beans);
                }
                setAttributes(cnxNode, o, beans, sNode);
                if(name != null && ! name.trim().isEmpty())
                    o.setName(name.trim());
                sNode.registerStarter(o);
                logger.debug(Util.delayedFormatString("Connexion registred: %s for %s", o, sNode));
            }
            catch (NoClassDefFoundError ex) {
                logger.warn("Connection class not found: " + type+ ": " + ex);
            }
            catch (ClassCastException ex) {
                logger.warn("didn't get a Connection but a " + o.getClass().getName());
            }
            catch (LinkageError ex) {
                logger.warn("Incompatible code version during connection creation of type " + type +
                        ": " + ex, ex);
            }
            catch (Exception ex) {
                logger.warn("Error during connection creation of type " + type +
                        ": " + ex, ex);
            }
        }
    }

    private void setAttributes(JrdsElement probeNode, Object o, Map<String, PropertyDescriptor> beans, Object... context) throws IllegalArgumentException, InvocationTargetException {
        //Resolve the beans
        for(JrdsElement attrNode: probeNode.getChildElementsByName("attr")) {
            String name = attrNode.getAttribute("name");
            PropertyDescriptor bean = beans.get(name);
            if(bean == null) {
                logger.error("Unknonw bean " + name);
                continue;
            }
            String textValue = Util.parseTemplate(attrNode.getTextContent(), context);
            logger.trace(Util.delayedFormatString("Fond attribute %s with value %s", name, textValue));
            try {
                Constructor<?> c = bean.getPropertyType().getConstructor(String.class);
                Object value = c.newInstance(textValue);
                bean.getWriteMethod().invoke(o, value);
            } catch (IllegalArgumentException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            } catch (SecurityException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            } catch (InstantiationException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            } catch (IllegalAccessException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            } catch (InvocationTargetException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            } catch (NoSuchMethodException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            }
        }

    }

    /**
     * @param pf the pf to set
     */
    void setProbeFactory(ProbeFactory pf) {
        this.pf = pf;
    }

    /**
     * @param macrosMap the macrosMap to set
     */
    void setMacros(Map<String, Macro> macrosMap) {
        this.macrosMap = macrosMap;
    }

    /**
     * @param classLoader the classLoader to set
     */
    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
