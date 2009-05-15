package jrds.factories;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.ArgFactory;
import jrds.GraphDesc;
import jrds.GraphFactory;
import jrds.Macro;
import jrds.ProbeDesc;
import jrds.ProbeFactory;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;

public class ConfigObjectFactory {
	static final private Logger logger = Logger.getLogger(ConfigObjectFactory.class);

	private final ArgFactory af = new ArgFactory();
	private GraphFactory gf;
	private ProbeFactory pf;
	private Loader l;
	private Map<Loader.ConfigType, ObjectBuilder> builderMap;
	private jrds.PropertiesManager pm = null;

	public ConfigObjectFactory(jrds.PropertiesManager pm, Loader l){
		this.l = l;
		this.pm = pm;
		
		builderMap = new HashMap<Loader.ConfigType, ObjectBuilder>(Loader.ConfigType.values().length);
		builderMap.put(Loader.ConfigType.FILTER, new FilterBuilder());
		builderMap.put(Loader.ConfigType.GRAPHDESC, new GraphDescBuilder());
		builderMap.put(Loader.ConfigType.HOSTS, new HostBuilder());
		builderMap.put(Loader.ConfigType.MACRODEF, new MacroBuilder());
		builderMap.put(Loader.ConfigType.PROBEDESC, new ProbeDescBuilder());
		builderMap.put(Loader.ConfigType.SUM, new SumBuilder());
		
		setProperty(ObjectBuilder.properties.ARGFACTORY, af);
		setProperty(ObjectBuilder.properties.CLASSLOADER, pm.extensionClassLoader);
	}
	
	public ConfigObjectFactory(jrds.PropertiesManager pm, Map<String, GraphDesc> graphDescMap, Map<String, ProbeDesc> probeDescMap, Loader l) throws ParserConfigurationException {
		gf = new GraphFactory(graphDescMap, pm.legacymode);
		pf = new ProbeFactory(probeDescMap, gf, pm, pm.legacymode);
		this.l = l;

		builderMap = new HashMap<Loader.ConfigType, ObjectBuilder>(Loader.ConfigType.values().length);
		builderMap.put(Loader.ConfigType.SUM, new SumBuilder());
		builderMap.put(Loader.ConfigType.HOSTS, new HostBuilder());
		builderMap.put(Loader.ConfigType.MACRODEF, new MacroBuilder());

		setProperty(ObjectBuilder.properties.ARGFACTORY, af);
		builderMap.get(Loader.ConfigType.HOSTS).setProperty(ObjectBuilder.properties.PROBEFACTORY, pf);
		builderMap.get(Loader.ConfigType.HOSTS).setProperty(ObjectBuilder.properties.GRAPHFACTORY, gf);

		builderMap.get(Loader.ConfigType.MACRODEF).setProperty(ObjectBuilder.properties.PROBEFACTORY, pf);
	}

	public Map<String, ?> getObjectMap(Loader.ConfigType ct) {
		Map<String, Object> objectMap = new HashMap<String, Object>();
		for(JrdsNode n: l.getRepository(ct).values()) {
			Object o = null;
			String name;
			name = n.evaluate(ct.getNameXpath());
			try {
				o = builderMap.get(ct).build(n);
				if(o != null) {
					objectMap.put(name, o);
				}
			} catch (Exception e) {
				logger.equals("Fatal error for object of type " + ct + " and name " + name + ":" + e);
			}
		}

		return objectMap;
	}

	public Object getObject(Loader.ConfigType ct, JrdsNode n ) {
		return builderMap.get(ct).build(n);
	}

	private void setProperty(ObjectBuilder.properties name, Object o) {
		for(ObjectBuilder ob: builderMap.values()) {
			ob.setProperty(name, o);
		}
	}
	public void setMacroMap(Map<String, Macro> macrosmap) {
		setProperty(ObjectBuilder.properties.MACRO, macrosmap);
	}

	public void setGraphDescMap(Map<String, Macro> macrosmap) {
		setProperty(ObjectBuilder.properties.GRAPHDESC, macrosmap);
	}
	public void setProbeDescMap(Map<String, Macro> macrosmap) {
		setProperty(ObjectBuilder.properties.PROBEDESC, macrosmap);
	}
	public void setGraphFactory(Map<String, GraphDesc> graphDescMap) {
		gf = new GraphFactory(graphDescMap, pm.legacymode);
		setProperty(ObjectBuilder.properties.GRAPHFACTORY, graphDescMap);
	}
	public void setProbeFactory(Map<String, ProbeDesc> probeDescMap) {
		pf = new ProbeFactory(probeDescMap, gf, pm, pm.legacymode);
		setProperty(ObjectBuilder.properties.PROBEFACTORY, pf);
	}
	
}
