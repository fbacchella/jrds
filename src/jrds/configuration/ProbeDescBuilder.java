package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.factories.ArgFactory;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

public class ProbeDescBuilder extends ConfigObjectBuilder<ProbeDesc> {
	static final private Logger logger = Logger.getLogger(ProbeDescBuilder.class);

	private ClassLoader classLoader = ProbeDescBuilder.class.getClassLoader();

    public ProbeDescBuilder() {
        super(ConfigType.PROBEDESC);
    }

    @Override
	ProbeDesc build(JrdsNode n) throws InvocationTargetException {
		try {
			return makeProbeDesc(n);
		} catch (SecurityException e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (IllegalArgumentException e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (NoSuchMethodException e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (IllegalAccessException e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (InvocationTargetException e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (ClassNotFoundException e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (NoClassDefFoundError e) {
			throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
		} catch (InstantiationException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        }
	}

	@SuppressWarnings("unchecked")
	public ProbeDesc makeProbeDesc(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
		ProbeDesc pd = new ProbeDesc();

		JrdsNode probeDescNode = n.getChild(CompiledXPath.get("/probedesc"));
		probeDescNode.setMethod(pd, CompiledXPath.get("probeName"), "setProbeName");
		probeDescNode.setMethod(pd, CompiledXPath.get("name"), "setName");

		logger.trace("Creating probe description " + pd.getName());

		String className = probeDescNode.evaluate(CompiledXPath.get("probeClass")).trim();
		Class<? extends Probe<?,?>> c = (Class<? extends Probe<?,?>>) classLoader.loadClass(className);
		pd.setProbeClass(c);

		pd.setHeartBeatDefault(pm.step * 2);

		probeDescNode.callIfExist(pd, CompiledXPath.get("uniq"), "setUniqIndex", Boolean.TYPE, true);
		probeDescNode.setMethod(pd, CompiledXPath.get("uptimefactor"), "setUptimefactor", Float.TYPE);

		List<String> graphs = probeDescNode.doTreeList(CompiledXPath.get("graphs/name"), new JrdsNode.FilterNode<String>() {
			@Override
			public String filter(Node input) {
				if(logger.isTraceEnabled())
					logger.trace("Adding graph: " + input.getTextContent());
				return input.getTextContent();
			}

		});
		pd.setGraphClasses(graphs);

		for(JrdsNode specificNode: probeDescNode.iterate(CompiledXPath.get("specific"))) {
			Map<String, String> m = specificNode.attrMap();
			if(m != null) {
				String name = m.get("name");
				String value = specificNode.getTextContent().trim();
				pd.addSpecific(name, value);
				logger.trace("Specific added: " + name + "='" + value + "'");
			}
		}

		String snmpRequester = probeDescNode.evaluate(CompiledXPath.get("snmpRequester"));
		if(snmpRequester != null && ! "".equals(snmpRequester.trim())) {
			pd.addSpecific("requester", snmpRequester.trim());
			logger.trace("Specific added: requester='" + snmpRequester.trim() + "'");

		}

		String index = probeDescNode.evaluate(CompiledXPath.get("index"));
		if(index !=null && ! "".equals(index))	{
			pd.addSpecific(jrds.probe.snmp.RdsIndexedSnmpRrd.INDEXOIDNAME, index.trim());
		}

		//Populating default argument vector
		JrdsNode argsNode = probeDescNode.getChild(CompiledXPath.get("defaultargs"));
		if(argsNode != null)
			for(Object o:  ArgFactory.makeArgs(argsNode)) {
				pd.addDefaultArg(o);
			}

		for(Map<String, Object> dsMap: doDsList(pd.getName(), probeDescNode)) {
			pd.add(dsMap);			
		}

		Map<String, String> props = makeProperties(probeDescNode);
		if(props != null)
			pd.setProperties(props);

		return pd;
	}

    /**
     * @param classLoader the classLoader to set
     */
    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
