package jrds.configuration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.ArchivesSet;
import jrds.ConnectedProbe;
import jrds.GenericBean;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostInfo;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.factories.xml.JrdsNode;
import jrds.probe.ContainerProbe;
import jrds.probe.PassiveProbe;
import jrds.starter.Connection;
import jrds.starter.ConnectionInfo;
import jrds.starter.HostStarter;
import jrds.starter.Listener;
import jrds.starter.Timer;
import jrds.store.StoreFactory;

import org.apache.log4j.Logger;

public class HostBuilder extends ConfigObjectBuilder<HostInfo> {
    static final private Logger logger = Logger.getLogger(HostBuilder.class);

    private ClassLoader classLoader = null;
    private ProbeFactory pf;
    private Map<String, Macro> macrosMap;
    private Map<String, Timer> timers = Collections.emptyMap();
    private Map<String, Listener<?, ?>> listeners = Collections.emptyMap();
    private Map<String, ArchivesSet> archivessetmap = Collections.singletonMap(ArchivesSet.DEFAULT.getName(), ArchivesSet.DEFAULT);

    private Map<String, GraphDesc> graphDescMap;

    public HostBuilder() {
        super(ConfigType.HOSTS);
    }

    @Override
    HostInfo build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeHost(n);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        } catch (ClassNotFoundException e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        }
    }

    public HostInfo makeHost(JrdsDocument n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        JrdsElement hostNode = n.getRootElement();
        String hostName = hostNode.getAttribute("name");
        String dnsHostname = hostNode.getAttribute("dnsName");
        if(hostName == null) {
            return null;
        }

        HostInfo host;
        if(dnsHostname != null) {
            host = new HostInfo(hostName, dnsHostname);
        }
        else {
            host = new HostInfo(hostName);
        }
        host.setHostDir(new File(pm.rrddir, host.getName()));

        String hidden = hostNode.getAttribute("hidden");
        host.setHidden(hidden != null && Boolean.parseBoolean(hidden));

        Map<String, Set<String>> collections = new HashMap<String, Set<String>>();

        parseFragment(hostNode, host, collections, null);

        return host;
    }

    private void parseFragment(JrdsElement fragment, HostInfo host, Map<String, Set<String>> collections, Map<String, String> properties) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // Find the connection for this host
        // Will the registered latter, in the starter node, one for each timer
        for(ConnectionInfo cnx: makeConnexion(fragment, host, properties)) {
            host.addConnection(cnx);
        }

        for(JrdsElement tagElem: fragment.getChildElementsByName("tag")) {
            logger.trace(Util.delayedFormatString("adding tag %s to %s", tagElem, host));
            String textContent = tagElem.getTextContent();
            if(textContent != null) {
                host.addTag(Util.parseTemplate(textContent.trim(), host, properties));                
            }
        }

        for(JrdsElement collectionNode: fragment.getChildElementsByName("collection")) {
            String name = collectionNode.getAttribute("name");
            Set<String> set = new HashSet<String>();
            for(JrdsElement e: collectionNode.getChildElementsByName("element")) {
                set.add(e.getTextContent());
            }
            collections.put(name, set);
            logger.trace(Util.delayedFormatString("adding collection %s with name %s to %s", set, name, host));
        }

        for(JrdsElement macroNode: fragment.getChildElementsByName("macro")) {
            String name = macroNode.getAttribute("name");
            Macro m = macrosMap.get(name);
            logger.trace(Util.delayedFormatString("Adding macro %s: %s", name, m));
            if(m != null) {
                Map<String, String> macroProps = makeProperties(macroNode, properties, host);
                logger.trace(Util.delayedFormatString("properties inherited for macro %s: %s", m, properties));
                logger.trace(Util.delayedFormatString("local properties for macro %s: %s", m, macroProps));
                Map<String, String> newProps = new HashMap<String, String>((properties !=null ? properties.size():0) + macroProps.size());
                if(properties != null)
                    newProps.putAll(properties);
                newProps.putAll(macroProps);
                JrdsDocument hostdoc = (JrdsDocument) fragment.getOwnerDocument();
                //Make a copy of the document fragment
                JrdsNode newDf = JrdsNode.build(hostdoc.importNode(m.getDf(), true));
                JrdsElement macrodef = JrdsNode.build( newDf.getFirstChild());
                parseFragment(macrodef, host, collections, newProps);
            }
            else {
                logger.error("Unknown macro:" + name);
            }
        }

        for(JrdsElement forNode: fragment.getChildElementsByName("for")) {
            Map<String, String> forattr = forNode.attrMap();
            String iterprop = forattr.get("var");
            Collection<String> set = null;
            String name = Util.parseTemplate(forNode.attrMap().get("collection"), this, properties);
            if(name != null) {
                set = collections.get(name);                
            }
            else if(forattr.containsKey("min") && forattr.containsKey("max") && forattr.containsKey("step")) {
                int min = Util.parseStringNumber(Util.parseTemplate(forattr.get("min"), this, properties), Integer.MAX_VALUE);
                int max = Util.parseStringNumber(Util.parseTemplate(forattr.get("max"), this, properties), Integer.MIN_VALUE);
                int step = Util.parseStringNumber(Util.parseTemplate(forattr.get("step"), this, properties), Integer.MIN_VALUE);
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
                for(String i: set) {
                    Map<String, String> temp;
                    if(properties != null) {
                        temp = new HashMap<String, String>(properties.size() +1);
                        temp.putAll(properties);
                        temp.put(iterprop, i);
                    }
                    else {
                        temp = Collections.singletonMap(iterprop, i);
                    }
                    logger.trace(Util.delayedFormatString("for using %s", temp));
                    parseFragment(forNode, host, collections, temp);
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
                makeProbe(probeNode, host, properties);
            } catch (Exception e) {
                logger.error("Probe creation failed for host " + host.getName() + ": ");
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();  
                e.printStackTrace(new PrintStream(buffer));
                logger.error(buffer);
            }
        }

        // Extract all the graph added at host level
        if(fragment.getElementbyName("graph") != null) {
            // They will be stored in a specific container probe
            Probe<?,?> graphprobe = new ContainerProbe("_NodeLevelGraph_", host);
            host.addProbe(graphprobe);
            for(JrdsElement graphNode: fragment.getChildElementsByName("graph")) {
                GraphDesc gd = graphDescMap.get(graphNode.getAttribute("type"));
                if(gd == null) {
                    logger.error(String.format("Graph %s not found for host %s", graphNode.getAttribute("type"), host.getName())); 
                    continue;
                }
                // Read the beans value for this graph and store them in a map
                // The map is used for template parsing and will be used for graph instantiation
                Map<String, String> attrs = new HashMap<String, String>(0);
                for(JrdsElement attrNode: graphNode.getChildElementsByName("attr")) {
                    String name = attrNode.getAttribute("name");
                    String value = Util.parseTemplate(attrNode.getTextContent(), host, gd);
                    attrs.put(name, value);
                }                
                GraphNode gn = new GraphNode(graphprobe, gd);
                gn.setBeans(attrs);
                graphprobe.addGraph(gn);
            }
        }
    }

    public Probe<?,?> makeProbe(JrdsElement probeNode, HostInfo host, Map<String, String> properties) throws InvocationTargetException {
        Probe<?,?> p = null;
        String type = probeNode.attrMap().get("type");

        List<Map<String, Object>> dsList = doDsList(type, probeNode.getElementbyName("dslist"));
        if(dsList.size() > 0) {
            logger.trace(Util.delayedFormatString("Data source replaced for %s/%s: %s", host, type, dsList));
            ProbeDesc oldpd = pf.getProbeDesc(type);
            try {
                ProbeDesc pd = (ProbeDesc) oldpd.clone();
                pd.replaceDs(dsList);
                p = pf.makeProbe(pd);
            } catch (CloneNotSupportedException e) {
                throw new InvocationTargetException(e, HostBuilder.class.getName());
            }
        }
        else {
            p = pf.makeProbe(type);
        }
        if(p == null)
            return null;

        p.readProperties(pm);

        String timerName = probeNode.getAttribute("timer");
        if(timerName == null)
            timerName = Timer.DEFAULTNAME;
        Timer timer = timers.get(timerName);
        if(timer == null) {
            logger.error("Invalid timer '" + timerName + "' for probe " + host.getName() + "/" + type);
            return null;
        }
        else {
            logger.trace(Util.delayedFormatString("probe %s/%s will use timer %s", host, type, timer));
        }
        p.setStep(timer.getStep());
        p.setTimeout(timer.getTimeout());

        // Identify the archive to use
        String archivesName;
        // Check if a custom archives list is defined
        if (probeNode.hasAttribute("archivesset")) {
            archivesName = probeNode.getAttribute("archivesset");
        }
        else {
            archivesName = pm.archivesSet;
        }
        if(archivesName == null || "".equals(archivesName) || ! archivessetmap.containsKey(archivesName)) {
            logger.error("invalid archives set name: " + archivesName);
            return null;
        }
        ArchivesSet archives = archivessetmap.get(archivesName);
        p.setArchives(archives);

        //The label is set
        String label = probeNode.getAttribute("label");
        if(label != null && ! "".equals(label)) {
            logger.trace(Util.delayedFormatString("Adding label %s to %s", label, p));
            p.setLabel(jrds.Util.parseTemplate(label, host, properties));
        }

        //The host is set
        HostStarter shost = timer.getHost(host);
        p.setHost(shost);

        ProbeDesc pd = p.getPd();
        List<Object> args = ArgFactory.makeArgs(probeNode, host, properties);
        //Prepare the probe with the default beans values
        Map<String, ProbeDesc.DefaultBean> defaultBeans = pd.getDefaultBeans();
        for(Map.Entry<String, ProbeDesc.DefaultBean> e: defaultBeans.entrySet()) {
            if(e.getValue().delayed) {
                continue;
            }
            String beanName = e.getKey();
            String beanValue = e.getValue().value;
            if(! resolveDefaultBean(p, args, properties, beanName, beanValue)) {
                return null;
            }
        }

        //Resolve the beans
        try {
            setAttributes(defaultBeans, probeNode, p, host, properties);
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Can't configure %s for %s: %s", pd.getName(), host, e));
            return null;
        }

        // Now evaluate the delayed default value parsing
        for(Map.Entry<String, ProbeDesc.DefaultBean> e: defaultBeans.entrySet()) {
            if(! e.getValue().delayed) {
                continue;
            }
            String beanName = e.getKey();
            String beanValue = e.getValue().value;
            if(! resolveDefaultBean(p, args, properties, beanName, beanValue)) {
                return null;
            }
        }

        if( !pf.configure(p, args)) {
            logger.error(p + " configuration failed");
            return null;
        }

        //A connected probe, register the needed connection
        //It can be defined within the node, referenced by it's name, or it's implied name
        if(p instanceof ConnectedProbe) {
            String connectionName;
            ConnectedProbe cp = (ConnectedProbe) p;
            //Register the connections defined within the probe
            for(ConnectionInfo ci: makeConnexion(probeNode, p, properties)) {
                ci.register(p);
            }
            String connexionName = probeNode.getAttribute("connection");
            if(connexionName != null && ! "".equals(connexionName)) {
                logger.trace(Util.delayedFormatString("Adding connection %s to %s", connexionName, p));
                connectionName = jrds.Util.parseTemplate(connexionName, host, properties);
                cp.setConnectionName(connectionName);
            }
            else {
                connectionName = cp.getConnectionName();
            }
            //If the connection is not already registred, try looking for it
            //And register it with the host
            if(p.find(connectionName) == null) {
                if(logger.isTraceEnabled())
                    logger.trace(Util.delayedFormatString("Looking for connection %s in %s", connectionName, host.getConnections()));
                ConnectionInfo ci = host.getConnection(connectionName);
                if(ci != null)
                    ci.register(shost);
                else {
                    logger.error(Util.delayedFormatString("Failed to find a connection %s for a probe %s", connectionName, cp));
                    return null;
                }
            }
        }

        //try {
        Map<String, String> empty = Collections.emptyMap();

        try {
            p.setMainStore(pm.defaultStore, empty);
        } catch (Exception e1) {
            logger.error(Util.delayedFormatString("Failed to configure the default store for the probe %s", pm.defaultStore.getClass(), p));
            return null;
        }

        for(Map.Entry<String, StoreFactory> e: pm.stores.entrySet()) {
            try {
                p.addStore(e.getValue());
            } catch (Exception e1) {
                logger.warn(Util.delayedFormatString("Failed to configure the store %s for the probe %s", e.getKey(), e.getValue().getClass().getCanonicalName(), p));
            }
        }

        //A passive probe, perhaps a specific listener is defined
        if(p instanceof PassiveProbe) {
            PassiveProbe<?> pp = (PassiveProbe<?>) p;
            String listenerName = probeNode.getAttribute("listener");
            if(listenerName != null && ! listenerName.trim().isEmpty()) {
                Listener<?, ?> l = listeners.get(listenerName);
                if(l != null) {
                    pp.setListener(l);
                }
                else {
                    logger.error(Util.delayedFormatString("Listener name not found for %s: %s", pp, listenerName));
                }
            }
        }

        if(p.checkStore()) {
            shost.addProbe(p);
        }
        else {
            return null;
        }

        return p;
    }

    private boolean resolveDefaultBean(Probe<?,?> p, List<Object> args, Map<String, String> properties, String beanName, String beanValue) {
        HostInfo host = p.getHost();
        ProbeDesc pd = p.getPd();
        GenericBean bean = pd.getBean(beanName);
        String value;
        //If the last argument is a list, give it to the template parser
        Object lastArgs = args.isEmpty() ? null : args.get(args.size() - 1);

        try {
            if(lastArgs instanceof List) {
                value = Util.parseTemplate(beanValue, host, p, lastArgs, properties);
            }
            else {
                value = Util.parseTemplate(beanValue, host, p, properties);
            }
        } catch (Exception e) {
            Throwable root = e;
            while(root.getCause() != null) {
                root = e.getCause();
            }
            logger.error(String.format("Probe %s: invalid bean %s template '%s': %s", pd.getName(), beanName, beanValue, root.getMessage()));
            return false;
        }
        logger.trace(Util.delayedFormatString("Adding attribute %s=%s (%s) to default args", beanName, value, value.getClass()));
        try {
            bean.set(p, value);
        } catch (Exception e) {
            Throwable root = e;
            while(root.getCause() != null) {
                root = e.getCause();
            }
            logger.error(String.format("Probe %s: invalid bean %s value '%s': %s", pd.getName(), beanName, beanValue, root.getMessage()));
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    /**
     * A compatibility method, snmp starter should be managed as a connection
     * @param node
     * @param p
     * @param host
     */
    private ConnectionInfo parseSnmp(JrdsElement node, Object parent, Map<String, String> properties) {
        try {
            JrdsElement snmpNode = node.getElementbyName("snmp");
            if(snmpNode != null) {
                logger.info("found an old snmp starter, please update to a connection");
                String connectionClassName = "jrds.snmp.SnmpConnection";
                Class<? extends Connection<?>> connectionClass = (Class<? extends Connection<?>>) pm.extensionClassLoader.loadClass(connectionClassName);

                Map<String, String> attrs = new HashMap<String, String>();
                for(Map.Entry<String, String> e: snmpNode.attrMap().entrySet()) {
                    attrs.put(e.getKey(), Util.parseTemplate(e.getValue(), parent, properties));
                }
                return new ConnectionInfo(connectionClass, connectionClassName, Collections.emptyList(), attrs);
            }
        } catch (ClassNotFoundException e) {
            logger.debug("Class jrds.snmp.SnmpConnection not found");
        } catch (Exception e) {
            logger.error("Error creating SNMP connection: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Enumerate the connections found in an XML node
     * @param domNode a node to parse
     * @param parent
     * @param properties
     * @return
     */
    Set<ConnectionInfo> makeConnexion(JrdsElement domNode, Object parent, Map<String, String> properties) {
        Set<ConnectionInfo> connectionSet = new HashSet<ConnectionInfo>();

        //Check for the old SNMP connection node
        ConnectionInfo cnxSnmp = parseSnmp(domNode, parent, properties);
        if(cnxSnmp != null)
            connectionSet.add(cnxSnmp);

        for(JrdsElement cnxNode: domNode.getChildElementsByName("connection")) {
            String type = cnxNode.getAttribute("type");
            if(type == null) {
                logger.error("No type declared for a connection");
                continue;
            }
            String name = Util.parseTemplate(cnxNode.getAttribute("name"), parent, properties);

            try {
                //Load the class for the connection
                @SuppressWarnings("unchecked")
                Class<? extends Connection<?>> connectionClass = (Class<? extends Connection<?>>) classLoader.loadClass(type);

                //Build the arguments vector for the connection
                List<Object> args = ArgFactory.makeArgs(cnxNode);

                //Resolve the bean for the connection
                Map<String, String> attrs = new HashMap<String, String>();
                for(JrdsElement attrNode: cnxNode.getChildElementsByName("attr")) {
                    String attrName = attrNode.getAttribute("name");
                    String textValue = Util.parseTemplate(attrNode.getTextContent(), parent, properties);
                    attrs.put(attrName, textValue);
                }
                ConnectionInfo cnx = new ConnectionInfo(connectionClass, name, args, attrs);
                connectionSet.add(cnx);
                logger.debug(Util.delayedFormatString("Added connection %s to node %s with beans %s", cnx, parent, attrs));
            }
            catch (ClassNotFoundException ex) {
                logger.warn("Connection class not found: " + type + " for " + parent);
            }
            catch (NoClassDefFoundError ex) {
                logger.warn("Connection class not found: " + type + ": " + ex);
            }
            catch (ClassCastException ex) {
                logger.warn(type + " is not a connection");
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
        return connectionSet;
    }

    private void setAttributes(Map<String, ProbeDesc.DefaultBean> defaultBeans, JrdsElement probeNode, Probe<?, ?> p, Object... context) throws IllegalArgumentException, InvocationTargetException {
        //Resolve the beans
        for(JrdsElement attrNode: probeNode.getChildElementsByName("attr")) {
            String name = attrNode.getAttribute("name");
            GenericBean bean = p.getPd().getBean(name);
            if(bean == null) {
                //Context[0] should be the host
                logger.error("Unknown bean '" + name + "' for " + context[0]);
                continue;
            }
            String textValue = Util.parseTemplate(attrNode.getTextContent(), context);
            logger.trace(Util.delayedFormatString("Found attribute %s with value %s", name, textValue));
            bean.set(p, textValue);
            if(defaultBeans.containsKey(name)) {
                defaultBeans.remove(name);
            }
        }
    }

    private Map<String, String> makeProperties(JrdsElement n, Object... o) {
        if(n == null)
            return Collections.emptyMap();
        JrdsElement propElem = n.getElementbyName("properties");
        if(propElem == null)
            return Collections.emptyMap();

        Map<String, String> props = new HashMap<String, String>();
        for(JrdsElement propNode: propElem.getChildElementsByName("entry")) {
            String key = propNode.getAttribute("key");
            if(key != null) {
                String value = propNode.getTextContent();
                value = Util.parseTemplate(value, o);
                logger.trace(Util.delayedFormatString("Adding propertie %s=%s", key, value));
                props.put(key, value);
            }
        }
        logger.debug(Util.delayedFormatString("Properties map: %s", props));
        return props;
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

    /**
     * @param timers
     */
    public void setTimers(Map<String, Timer> timers) {
        this.timers = timers;
    }

    public void setListeners(Map<String, Listener<?, ?>> listenerMap) {
        listeners = listenerMap;
    }

    public void setGraphDescMap(Map<String, GraphDesc> graphDescMap) {
        this.graphDescMap = graphDescMap;
    }

    public void setArchivesSetMap(Map<String, ArchivesSet> archivessetmap) {
        logger.debug(Util.delayedFormatString("will look for archives in %s", archivessetmap));
        this.archivessetmap = archivessetmap;        
    }

}
