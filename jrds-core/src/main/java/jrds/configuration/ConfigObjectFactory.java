package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.ArchivesSet;
import jrds.Filter;
import jrds.GraphDesc;
import jrds.HostInfo;
import jrds.Macro;
import jrds.ProbeDesc;
import jrds.Tab;
import jrds.Util;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.graphe.Sum;
import jrds.starter.Listener;
import jrds.starter.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class ConfigObjectFactory {
    static final private Logger logger = LoggerFactory.getLogger(ConfigObjectFactory.class);

    private ProbeFactory pf;
    private ClassLoader cl = this.getClass().getClassLoader();
    private Map<String, GraphDesc> graphDescMap = Collections.emptyMap();
    private Map<String, Listener<?, ?>> listenerMap = Collections.emptyMap();
    Map<String, Macro> macrosmap = Collections.emptyMap();
    Map<String, ArchivesSet> archivessetmap = Collections.singletonMap(ArchivesSet.DEFAULT.getName(), ArchivesSet.DEFAULT);
    private final jrds.PropertiesManager pm;
    private Loader load = null;

    @Getter @Setter @Accessors(chain=true)
    private ProbeClassResolver probeClassResolver;

    public ConfigObjectFactory(jrds.PropertiesManager pm) {
        this.pm = pm;
        this.cl = pm.extensionClassLoader;
        this.probeClassResolver = new ProbeClassResolver(pm.extensionClassLoader);
        init();
    }

    public ConfigObjectFactory(jrds.PropertiesManager pm, ClassLoader cl) {
        this.pm = pm;
        this.cl = cl;
        this.probeClassResolver = new ProbeClassResolver(cl);
        init();
    }

    private void init() {
        load = new Loader(pm.strictparsing);

        logger.debug("Scanning {} for probes libraries", pm.libspath);
        for(URI lib: pm.libspath) {
            logger.info("Adding lib {}", lib);
            load.importUrl(lib);
        }

        if(pm.configdir != null)
            load.importDir(pm.configdir);

        load.done();
    }

    public void addUrl(URI ressourceUrl) {
        load.importUrl(ressourceUrl);
    }

    public Map<String, JrdsDocument> getNodeMap(ConfigType ct) {
        return load.getRepository(ct);
    }

    public <BuildObject> Map<String, BuildObject> getObjectMap(ConfigObjectBuilder<BuildObject> ob, Map<String, JrdsDocument> nodeMap) {
        Map<String, BuildObject> objectMap = new HashMap<>();

        for(Map.Entry<String, JrdsDocument> e: nodeMap.entrySet()) {
            JrdsDocument n = e.getValue();
            BuildObject o;
            String name = ob.ct.getName(n);
            try {
                o = ob.build(n);
                if(o != null && name != null) {
                    objectMap.put(name, o);
                }
            } catch (InvocationTargetException ex) {
                logger.error("Fatal error for object of type {} and name {}: {}", ob.ct, name, Util.resolveThrowableException(ex.getCause()));
                logger.debug("Cause", ex);
            }
            // Remove DOM object as soon as it's not needed any more
            nodeMap.remove(e.getKey());
        }
        return objectMap;
    }

    public Map<String, ArchivesSet> setArchiveSetMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.ARCHIVESSET);
        archivessetmap = getObjectMap(new ArchivesSetBuilder(), nodemap);
        archivessetmap.put(ArchivesSet.DEFAULT.getName(), ArchivesSet.DEFAULT);
        logger.debug("Archives set configured: {}", Util.delayedFormatString(archivessetmap::keySet));
        return archivessetmap;
    }

    public Map<String, Macro> setMacroMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.MACRODEF);
        macrosmap = getObjectMap(new MacroBuilder(), nodemap);
        logger.debug("Macro configured: {}", Util.delayedFormatString(macrosmap::keySet));
        return macrosmap;
    }

    public Map<String, GraphDesc> setGrapMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.GRAPH);
        GraphDescBuilder ob = new GraphDescBuilder();
        ob.setPm(pm);
        Map<String, GraphDesc> graphsMap = getObjectMap(ob, nodemap);
        logger.debug("Graphs configured: {}", Util.delayedFormatString(graphsMap::keySet));
        return graphsMap;
    }

    public Map<String, GraphDesc> setGraphDescMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.GRAPHDESC);
        GraphDescBuilder ob = new GraphDescBuilder();
        ob.setPm(pm);
        graphDescMap = getObjectMap(ob, nodemap);
        logger.debug("Graph description configured: {}", Util.delayedFormatString(graphDescMap::keySet));
        return graphDescMap;
    }

    public Map<String, ProbeDesc<?>> setProbeDescMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.PROBEDESC);
        ProbeDescBuilder ob = new ProbeDescBuilder();
        ob.setPm(pm);
        ob.setGraphDescMap(graphDescMap);
        ob.setProbeClassResolver(probeClassResolver);
        Map<String, ProbeDesc<?>> probeDescMap = getObjectMap(ob, nodemap);
        pf = new ProbeFactory(probeDescMap, graphDescMap);
        logger.debug("Probe description configured:{}", Util.delayedFormatString(probeDescMap::keySet));
        return probeDescMap;
    }

    public Map<String, HostInfo> setHostMap(Map<String, Timer> timers) {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.HOSTS);
        HostBuilder ob = new HostBuilder();
        ob.setClassLoader(cl);
        ob.setMacros(macrosmap);
        ob.setProbeFactory(pf);
        ob.setPm(pm);
        ob.setTimers(timers);
        ob.setListeners(listenerMap);
        ob.setGraphDescMap(graphDescMap);
        ob.setArchivesSetMap(archivessetmap);
        Map<String, HostInfo> hostsMap = getObjectMap(ob, nodemap);
        logger.debug("Hosts configured: {}", Util.delayedFormatString(hostsMap::keySet));
        return hostsMap;
    }

    public Map<String, Filter> setFilterMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.FILTER);
        FilterBuilder ob = new FilterBuilder();
        ob.setPm(pm);
        Map<String, Filter> filtersMap = getObjectMap(ob, nodemap);
        logger.debug("Filters configured: {}", Util.delayedFormatString(filtersMap::keySet));
        return filtersMap;
    }

    public Map<String, Sum> setSumMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.SUM);
        SumBuilder ob = new SumBuilder();
        ob.setPm(pm);
        Map<String, Sum> sumpsMap = getObjectMap(ob, nodemap);
        logger.debug("Sums configured: {}", Util.delayedFormatString(sumpsMap::keySet));
        return sumpsMap;
    }

    public Map<String, Tab> setTabMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.TAB);
        TabBuilder ob = new TabBuilder();
        Map<String, Tab> tabsMap = getObjectMap(ob, nodemap);
        logger.debug("Tabs configured: {}", Util.delayedFormatString(tabsMap::keySet));
        return tabsMap;
    }

    public Map<String, Listener<?, ?>> setListenerMap() {
        Map<String, JrdsDocument> nodemap = load.getRepository(ConfigType.LISTENER);
        ListenerBuilder ob = new ListenerBuilder();
        ob.setClassLoader(cl);
        listenerMap = getObjectMap(ob, nodemap);
        logger.debug("Listener configured: {}", Util.delayedFormatString(listenerMap::keySet));
        return listenerMap;
    }

    /**
     * @return the loader
     */
    Loader getLoader() {
        return load;
    }

}
