package jrds.configuration;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.ArchivesSet;
import jrds.ConnectedProbe;
import jrds.GenericBean;
import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.HostInfo;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.ProbeDesc.DataSourceBuilder;
import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.AbstractJrdsNode;
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

public class HostBuilder extends ConfigObjectBuilder<HostInfo> {

    static final private Logger logger = LoggerFactory.getLogger(HostBuilder.class);

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
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException  e) {
            throw new InvocationTargetException(e, HostBuilder.class.getName());
        }
    }

    public HostInfo makeHost(JrdsDocument n) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        JrdsElement hostNode = n.getRootElement();
        String hostName = hostNode.getAttribute("name");
        String dnsHostname = hostNode.getAttribute("dnsName");
        if(hostName == null) {
            return null;
        }

        HostInfo host;
        if(dnsHostname != null) {
            host = new HostInfo(hostName, dnsHostname);
        } else {
            host = new HostInfo(hostName);
        }
        host.setHostDir(new File(pm.rrddir, host.getName()));

        String hidden = hostNode.getAttribute("hidden");
        host.setHidden(hidden != null && Boolean.parseBoolean(hidden));

        Map<String, Set<String>> collections = new HashMap<String, Set<String>>();

        parseFragment(hostNode, host, collections, null);

        return host;
    }

    private void parseFragment(JrdsElement fragment, HostInfo host, Map<String, Set<String>> collections, Map<String, String> properties) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // Find the connection for this host
        // Will the registered latter, in the starter node, one for each timer
        for(ConnectionInfo cnx: makeConnexion(fragment, host, properties)) {
            host.addConnection(cnx);
        }

        for(JrdsElement tagElem: fragment.getChildElementsByName("tag")) {
            logger.trace("adding tag {} to {}", tagElem, host);
            String textContent = tagElem.getTextContent();
            if(textContent != null) {
                host.addTag(Util.parseTemplate(textContent.trim(), host, properties, pm.secrets));
            }
        }

        for(JrdsElement collectionNode: fragment.getChildElementsByName("collection")) {
            String name = collectionNode.getAttribute("name");
            Set<String> set = new HashSet<String>();
            for(JrdsElement e: collectionNode.getChildElementsByName("element")) {
                set.add(e.getTextContent());
            }
            collections.put(name, set);
            logger.trace("adding collection {} with name {} to {}", set, name, host);
        }

        for(JrdsElement macroNode: fragment.getChildElementsByName("macro")) {
            String name = macroNode.getAttribute("name");
            Macro m = macrosMap.get(name);
            logger.trace("Adding macro {}: {}", name, m);
            if (m != null) {
                Map<String, String> macroProps = makeProperties(macroNode, properties, host, pm.secrets);
                logger.trace("properties inherited for macro {}: {}", m, properties);
                logger.trace("local properties for macro {}: {}", m, macroProps);
                Map<String, String> newProps = new HashMap<String, String>((properties != null ? properties.size() : 0) + macroProps.size());
                if(properties != null)
                    newProps.putAll(properties);
                newProps.putAll(macroProps);
                JrdsDocument hostdoc = (JrdsDocument) fragment.getOwnerDocument();
                // Make a copy of the document fragment
                JrdsNode newDf = AbstractJrdsNode.build(hostdoc.importNode(m.getDf(), true));
                JrdsElement macrodef = AbstractJrdsNode.build(newDf.getFirstChild());
                parseFragment(macrodef, host, collections, newProps);
            } else {
                logger.error("Unknown macro: {}", name);
            }
        }

        for (JrdsElement forNode: fragment.getChildElementsByName("for")) {
            Map<String, String> forattr = forNode.attrMap();
            String iterprop = forattr.get("var");
            Collection<String> set = null;
            String name = Util.parseTemplate(forNode.attrMap().get("collection"), this, properties, pm.secrets);
            if (name != null) {
                set = collections.get(name);
            } else if (forattr.containsKey("min") && forattr.containsKey("max") && forattr.containsKey("step")) {
                int min = Util.parseStringNumber(Util.parseTemplate(forattr.get("min"), this, properties, pm.secrets), Integer.MAX_VALUE);
                int max = Util.parseStringNumber(Util.parseTemplate(forattr.get("max"), this, properties, pm.secrets), Integer.MIN_VALUE);
                int step = Util.parseStringNumber(Util.parseTemplate(forattr.get("step"), this, properties, pm.secrets), Integer.MIN_VALUE);
                if (min > max || step <= 0) {
                    logger.error("invalid range from{} to {} with step {}", min, max, step);
                    break;
                }
                set = new ArrayList<String>((max - min) / step + 1);
                for (int i = min; i <= max; i += step) {
                    set.add(Integer.toString(i));
                }
            }

            if(set != null) {
                for(String i: set) {
                    Map<String, String> temp;
                    if(properties != null) {
                        temp = new HashMap<String, String>(properties.size() + 1);
                        temp.putAll(properties);
                        temp.put(iterprop, i);
                    } else {
                        temp = Collections.singletonMap(iterprop, i);
                    }
                    logger.trace("for using {}", temp);
                    parseFragment(forNode, host, collections, temp);
                }
            } else {
                logger.error("Invalid host configuration, collection {} not found", name);
            }
        }

        for(JrdsElement probeNode: fragment.getChildElements()) {
            if(!"probe".equals(probeNode.getNodeName()) && !"rrd".equals(probeNode.getNodeName()))
                continue;
            try {
                makeProbe(probeNode, host, properties);
            } catch (InvocationTargetException e) {
                logger.error("Probe creation failed for host {}: {}", host.getName(), Util.resolveThrowableException(e.getCause()));
                logger.debug("Cause", e);
            } catch (Exception e) {
                logger.error("Probe creation failed for host {}: {}", host.getName(), Util.resolveThrowableException(e.getCause()));
                logger.error("Cause", e);
            }
        }

        // Extract all the graph added at host level
        if(fragment.getElementbyName("graph") != null) {
            // They will be stored in a specific container probe
            Probe<?, ?> graphprobe = new ContainerProbe("_NodeLevelGraph_", host);
            host.addProbe(graphprobe);
            for(JrdsElement graphNode: fragment.getChildElementsByName("graph")) {
                GraphDesc gd = graphDescMap.get(graphNode.getAttribute("type"));
                if(gd == null) {
                    logger.error("Graph {} not found for host {}", graphNode.getAttribute("type"), host.getName());
                    continue;
                }
                // Read the beans value for this graph and store them in a map
                // The map is used for template parsing and will be used for
                // graph instantiation
                Map<String, String> attrs = new HashMap<String, String>(0);
                for(JrdsElement attrNode: graphNode.getChildElementsByName("attr")) {
                    String name = attrNode.getAttribute("name");
                    String value = Util.parseTemplate(attrNode.getTextContent(), host, gd, pm.secrets);
                    attrs.put(name, value);
                }
                GraphNode gn = new GraphNode(graphprobe, gd);
                gn.setBeans(attrs);
                graphprobe.addGraph(gn);
            }
        }
    }

    public Probe<?, ?> makeProbe(JrdsElement probeNode, HostInfo host, Map<String, String> properties) throws InvocationTargetException {
        Probe<?, ?> p = null;
        String type = probeNode.attrMap().get("type");
        type = jrds.Util.parseTemplate(type, host, properties, pm.secrets);

        List<DataSourceBuilder> dsList = doDsList(type, probeNode.getElementbyName("dslist"));
        if(dsList.size() > 0) {
            logger.trace("Data source replaced for {}/{}: {}", host, type, dsList);
            ProbeDesc<?> oldpd = pf.getProbeDesc(type);
            ProbeDesc<?> pd = new ProbeDesc<>(oldpd, dsList);
            p = pf.makeProbe(pd);
        } else {
            p = pf.makeProbe(type);
        }
        if(p == null) {
            return null;
        }

        p.readProperties(pm);

        String timerName = probeNode.getAttribute("timer");
        if(timerName == null) {
            timerName = Timer.DEFAULTNAME;
        }
        timerName = Util.parseTemplate(timerName, properties, p, host, pm.secrets);
        Timer timer = timers.get(timerName);
        if(timer == null) {
            logger.error("Invalid timer '" + timerName + "' for probe " + host.getName() + "/" + type);
            return null;
        } else {
            logger.trace("probe {}/{} will use timer {}", host, type, timer);
        }
        p.setStep(timer.getStep());
        p.setTimeout(timer.getTimeout());
        p.setSlowCollectTime(timer.getSlowCollectTime());

        // Identify the archive to use
        String archivesName;
        // Check if a custom archives list is defined
        if(probeNode.hasAttribute("archivesset")) {
            archivesName = Optional.ofNullable(probeNode.getAttribute("archivesset")).filter(s -> ! s.isEmpty()).orElse(null);
            if(archivesName == null) {
                logger.error("Empty archives set name");
                return null;
            }
        } else {
            archivesName = pm.archivesSet;
        }
        archivesName = Util.parseTemplate(archivesName, properties, p, host, pm.secrets);
        if(!archivessetmap.containsKey(archivesName)) {
            logger.error("Invalid archives set name: " + archivesName);
            return null;
        }
        ArchivesSet archives = archivessetmap.get(archivesName);
        p.setArchives(archives);

        // The label is set
        String label = probeNode.getAttribute("label");
        if(label != null && !"".equals(label)) {
            logger.trace("Adding label {} to {}", label, p);
            p.setLabel(jrds.Util.parseTemplate(label, properties, p, host, pm.secrets));
        }

        // The host is set
        HostStarter shost = timer.getHost(host);
        p.setHost(shost);

        ProbeDesc<?> pd = p.getPd();
        List<Object> args = ArgFactory.makeArgs(probeNode, host, properties, pm.secrets);
        // Prepare the probe with the default beans values
        Map<String, ProbeDesc.DefaultBean> defaultBeans = new HashMap<>(pd.getDefaultBeans());
        for(Map.Entry<String, ProbeDesc.DefaultBean> e: defaultBeans.entrySet()) {
            if(e.getValue().delayed) {
                continue;
            }
            String beanName = e.getKey();
            String beanValue = e.getValue().value;
            if(!resolveDefaultBean(p, args, properties, beanName, beanValue)) {
                return null;
            }
        }

        // Resolve the beans
        try {
            setAttributes(p, defaultBeans, probeNode, host, properties, pm.secrets);
        } catch (RuntimeException e) {
            logger.error(String.format("Can't configure %s for %s: %s", pd.getName(), host, e));
            return null;
        }

        // Now evaluate the delayed default value parsing
        for(Map.Entry<String, ProbeDesc.DefaultBean> e: defaultBeans.entrySet()) {
            if(!e.getValue().delayed) {
                continue;
            }
            String beanName = e.getKey();
            String beanValue = e.getValue().value;
            if(!resolveDefaultBean(p, args, properties, beanName, beanValue)) {
                return null;
            }
        }

        // Resolve the nested connections
        for (ConnectionInfo ci: makeConnexion(probeNode, p, properties)) {
            ci.register(p);
        }

        // Resolve the eventual connection name
        String connectionName = probeNode.getAttribute("connection");

        if (p instanceof ConnectedProbe) {
            ConnectedProbe cp = (ConnectedProbe) p;
            if (connectionName != null) {
                connectionName = jrds.Util.parseTemplate(connectionName, host, properties, args, pm.secrets);
                logger.trace("Setting connection {} used by {}/{}", connectionName, host, p);
                cp.setConnectionName(connectionName);
            } else {
                // connectionName resolves to the default connection name
                connectionName = cp.getConnectionName();
            }
            // If the connection is not already registered, try searching it in the host
            // and register it with the host's starter node. It's need because the probe is not yet linked to the host
            if (p.find(connectionName) == null) {
                logger.trace("Looking for connection {} in {}", connectionName, Util.delayedFormatString(host::getConnections));
                ConnectionInfo ci = host.getConnection(connectionName);
                if (ci != null) {
                    ci.register(shost);
                }
            }
        } else if (connectionName != null) {
            logger.warn("Useless connection defined on the not connected probe {}, it will be ignored", p);
        }

        if(!pf.configure(p, args)) {
            logger.error(p + " configuration failed");
            return null;
        }

        p.setOptionalsCollect();

        try {
            p.setMainStore(pm.defaultStore, Collections.emptyMap());
        } catch (InvocationTargetException ex) {
            logger.error("Failed to configure the default store {} for the probe {}: {}", pm.defaultStore.getClass(), p, Util.resolveThrowableException(ex));
            return null;
        }

        for(Map.Entry<String, StoreFactory> e: pm.stores.entrySet()) {
            try {
                p.addStore(e.getValue());
            } catch (Exception ex) {
                logger.warn("Failed to configure the store {} for the probe {}: {}",
                            e.getKey(), Util.delayedFormatString(() -> e.getValue().getClass().getCanonicalName()), p, Util.resolveThrowableException(ex));
            }
        }

        // A passive probe, perhaps a specific listener is defined
        if(p instanceof PassiveProbe) {
            PassiveProbe<?> pp = (PassiveProbe<?>) p;
            String listenerName = probeNode.getAttribute("listener");
            if(listenerName != null && !listenerName.trim().isEmpty()) {
                Listener<?, ?> l = listeners.get(listenerName);
                if(l != null) {
                    pp.setListener(l);
                } else {
                    logger.error("Listener name not found for {}: {}", pp, listenerName);
                }
            }
        }

        if(p.checkStore()) {
            shost.addProbe(p);
        } else {
            return null;
        }

        // Check that the probe can really find the requested connection
        // Done after that the probe is registered to is host, and after configuration, because some probes can generate their own connection
        if (p instanceof ConnectedProbe) {
            ConnectedProbe cp = (ConnectedProbe) p;
            if (cp.getConnectionName() != null && p.find(cp.getConnectionName()) == null) {
                logger.error("Failed to find a connection {} for a probe {}", cp.getConnectionName(), p);
                return null;
            }
        }
        return p;
    }

    private boolean resolveDefaultBean(Probe<?, ?> p, List<Object> args, Map<String, String> properties, String beanName, String beanValue) {
        HostInfo host = p.getHost();
        ProbeDesc<?> pd = p.getPd();
        GenericBean bean = pd.getBean(beanName);
        String value;
        // If the last argument is a list, give it to the template parser
        Object lastArgs = args.isEmpty() ? null : args.get(args.size() - 1);

        try {
            if(lastArgs instanceof List) {
                value = Util.parseTemplate(beanValue, host, p, lastArgs, properties, pm.secrets);
            } else {
                value = Util.parseTemplate(beanValue, host, p, properties, pm.secrets);
            }
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = e.getCause();
            }
            logger.error("Probe {}: invalid bean {} template {}': {}", pd.getName(), beanName, beanValue, root.getMessage());
            return false;
        }
        logger.trace("Adding attribute {}={} ({}) to default args", beanName, value, value.getClass());
        try {
            bean.set(p, value);
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = e.getCause();
            }
            logger.error("Probe {}: invalid bean {} value '{}': {}", pd.getName(), beanName, beanValue, root.getMessage());
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    /**
     * A compatibility method, snmp starter should be managed as a connection
     * 
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
                    attrs.put(e.getKey(), Util.parseTemplate(e.getValue(), parent, properties, pm.secrets));
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
     * 
     * @param domNode a node to parse
     * @param parent
     * @param properties
     * @return
     */
    Set<ConnectionInfo> makeConnexion(JrdsElement domNode, Object parent, Map<String, String> properties) {
        Set<ConnectionInfo> connectionSet = new HashSet<ConnectionInfo>();

        // Check for the old SNMP connection node
        ConnectionInfo cnxSnmp = parseSnmp(domNode, parent, properties);
        if(cnxSnmp != null)
            connectionSet.add(cnxSnmp);

        for(JrdsElement cnxNode: domNode.getChildElementsByName("connection")) {
            String type = cnxNode.getAttribute("type");
            if(type == null) {
                logger.error("No type declared for a connection");
                continue;
            }
            String name = Util.parseTemplate(cnxNode.getAttribute("name"), parent, properties, pm.secrets);

            try {
                // Load the class for the connection
                @SuppressWarnings("unchecked")
                Class<? extends Connection<?>> connectionClass = (Class<? extends Connection<?>>) classLoader.loadClass(type);

                // Build the arguments vector for the connection
                List<Object> args = ArgFactory.makeArgs(cnxNode, pm.secrets);

                // Resolve the bean for the connection
                Map<String, String> attrs = new HashMap<String, String>();
                for(JrdsElement attrNode: cnxNode.getChildElementsByName("attr")) {
                    String attrName = attrNode.getAttribute("name");
                    String textValue = Util.parseTemplate(attrNode.getTextContent(), parent, properties, pm.secrets);
                    attrs.put(attrName, textValue);
                }
                ConnectionInfo cnx = new ConnectionInfo(connectionClass, name, args, attrs);
                connectionSet.add(cnx);
                logger.debug("Added connection {} to node {} with beans {}", cnx, parent, attrs);
            } catch (ClassNotFoundException ex) {
                logger.warn("Connection class not found: " + type + " for " + parent);
            } catch (NoClassDefFoundError ex) {
                logger.warn("Connection class not found: " + type + ": " + ex);
            } catch (ClassCastException ex) {
                logger.warn(type + " is not a connection");
            } catch (LinkageError ex) {
                logger.warn("Incompatible code version during connection creation of type " + type + ": " + ex, ex);
            } catch (Exception ex) {
                logger.warn("Error during connection creation of type " + type + ": " + ex, ex);
            }
        }
        return connectionSet;
    }

    private void setAttributes(Probe<?, ?> p, Map<String, ProbeDesc.DefaultBean> defaultBeans, JrdsElement probeNode, Object... context) {
        // Resolve the beans
        for(JrdsElement attrNode: probeNode.getChildElementsByName("attr")) {
            String name = attrNode.getAttribute("name");
            GenericBean bean = p.getPd().getBean(name);
            if(bean == null) {
                // Context[0] should be the host
                logger.error(context[0] + "/" + p.getPd().getName() + ": unknown bean '" + name + "'");
                continue;
            }
            String textValue = Util.parseTemplate(attrNode.getTextContent(), context);
            logger.trace("Found attribute {} with value {}", name, textValue);
            bean.set(p, textValue);
            if (defaultBeans.containsKey(name)) {
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
                logger.trace("Adding propertie {}={}", key, value);
                props.put(key, value);
            }
        }
        logger.debug("Properties map: {}", props);
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
        logger.debug("will look for archives in {}", archivessetmap);
        this.archivessetmap = archivessetmap;
    }

}
