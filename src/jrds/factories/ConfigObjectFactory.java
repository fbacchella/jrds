package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jrds.Filter;
import jrds.GraphDesc;
import jrds.Macro;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.factories.xml.JrdsNode;
import jrds.probe.SumProbe;

import org.apache.log4j.Logger;

public class ConfigObjectFactory {
	static final private Logger logger = Logger.getLogger(ConfigObjectFactory.class);

	private ProbeFactory pf;
	private Map<String, GraphDesc> graphDescMap;
	private Map<Loader.ConfigType, ObjectBuilder> builderMap;
	private jrds.PropertiesManager pm = null;

	public ConfigObjectFactory(jrds.PropertiesManager pm){
		this.pm = pm;
		init(getClass().getClassLoader());
	}	
	public ConfigObjectFactory(jrds.PropertiesManager pm, ClassLoader cl){
		this.pm = pm;
		init(cl);
	}	
	
	private void init(ClassLoader cl) {
		builderMap = new HashMap<Loader.ConfigType, ObjectBuilder>(Loader.ConfigType.values().length);
		builderMap.put(Loader.ConfigType.FILTER, new FilterBuilder());
		builderMap.put(Loader.ConfigType.GRAPHDESC, new GraphDescBuilder());
		builderMap.put(Loader.ConfigType.GRAPH, new GraphDescBuilder());
		builderMap.put(Loader.ConfigType.HOSTS, new HostBuilder());
		builderMap.put(Loader.ConfigType.MACRODEF, new MacroBuilder());
		builderMap.put(Loader.ConfigType.PROBEDESC, new ProbeDescBuilder());
		builderMap.put(Loader.ConfigType.SUM, new SumBuilder());
		
		setProperty(ObjectBuilder.properties.CLASSLOADER, cl);
		setProperty(ObjectBuilder.properties.PM, pm);
	}

	public Map<String, ?> getObjectMap(Loader.ConfigType ct, Map<String, JrdsNode> nodeMap) {
		Map<String, Object> objectMap = new HashMap<String, Object>();
		for(JrdsNode n: nodeMap.values()) {
			Object o = null;
			String name;
			name = n.evaluate(ct.getNameXpath());
			try {
				o = builderMap.get(ct).build(n);
				if(o != null) {
					objectMap.put(name, o);
				}
			} catch (InvocationTargetException e) {
				logger.error("Fatal error for object of type " + ct + " and name " + name + ":" + e.getCause());
			}
		}
		return objectMap;
	}

	public Object getObject(Loader.ConfigType ct, JrdsNode n )  throws InvocationTargetException {
		return builderMap.get(ct).build(n);
	}

	private void setProperty(ObjectBuilder.properties name, Object o) {
		for(ObjectBuilder ob: builderMap.values()) {
			ob.setProperty(name, o);
		}
	}
	@SuppressWarnings("unchecked")
	public Map<String, Macro> setMacroMap(Map<String, JrdsNode> nodemap) {
		Map<String, Macro> macrosmap = (Map<String, Macro>) getObjectMap(Loader.ConfigType.MACRODEF, nodemap);
		setProperty(ObjectBuilder.properties.MACRO, macrosmap);
		logger.debug("Macro configured: " + macrosmap.keySet());
		return macrosmap;
	}
	@SuppressWarnings("unchecked")
	public Map<String, GraphDesc> setGraphDescMap(Map<String, JrdsNode> nodemap) {
		graphDescMap = (Map<String, GraphDesc>) getObjectMap(Loader.ConfigType.GRAPHDESC, nodemap);
		setProperty(ObjectBuilder.properties.GRAPHDESC, graphDescMap);
		setProperty(ObjectBuilder.properties.GRAPHFACTORY, graphDescMap);
		logger.debug("Graph description configured: " + graphDescMap.keySet());
		return graphDescMap;
	}
	@SuppressWarnings("unchecked")
	public Map<String, ProbeDesc> setProbeDescMap(Map<String, JrdsNode> nodemap) {
		Map<String, ProbeDesc> probeDescMap = (Map<String, ProbeDesc>) getObjectMap(Loader.ConfigType.PROBEDESC, nodemap);
		setProperty(ObjectBuilder.properties.PROBEDESC, probeDescMap);
		pf = new ProbeFactory(probeDescMap, graphDescMap, pm);
		setProperty(ObjectBuilder.properties.PROBEFACTORY, pf);
		logger.debug("Probe description configured: " + probeDescMap.keySet());
		return probeDescMap;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, RdsHost> setHostMap(Map<String, JrdsNode> hostmap) {
		Map<String, RdsHost> hostsMap = (Map<String, RdsHost>)getObjectMap(Loader.ConfigType.HOSTS, hostmap);
		logger.debug("Hosts configured: " + hostsMap.keySet());
		return hostsMap;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Filter> setFilterMap(Map<String, JrdsNode> filtermap) {
		Map<String, Filter> filtersMap = (Map<String, Filter>)getObjectMap(Loader.ConfigType.FILTER, filtermap);
		logger.debug("Filters configured: " + filtersMap.keySet());
		return filtersMap;
	}
	@SuppressWarnings("unchecked")
	public Map<String, SumProbe> setSumMap(Map<String, JrdsNode> summap) {
		Map<String, SumProbe> sumpsMap = (Map<String, SumProbe>)getObjectMap(Loader.ConfigType.SUM, summap);
		logger.debug("Sums configured: " + sumpsMap.keySet());
		return sumpsMap;
	}
	@SuppressWarnings("unchecked")
	public Map<String, GraphDesc> setGrapMap(Map<String, JrdsNode> graphmap) {
		Map<String, GraphDesc> graphsMap = (Map<String, GraphDesc>)getObjectMap(Loader.ConfigType.GRAPH, graphmap);
		logger.debug("Graphs configured: " + graphsMap.keySet());
		return graphsMap;
	}

	public Set<Class<?>> getPreloadedClass() {
		return pf.getPreloadedClass();
	}
}
