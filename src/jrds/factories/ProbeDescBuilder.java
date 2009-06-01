package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;
import org.rrd4j.DsType;
import org.snmp4j.smi.OID;
import org.w3c.dom.Node;

public class ProbeDescBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(ProbeDescBuilder.class);

	private ClassLoader classLoader = ProbeDescBuilder.class.getClassLoader();

	@Override
	Object build(JrdsNode n) throws InvocationTargetException {
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
		}
	}

	public ProbeDesc makeProbeDesc(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		ProbeDesc pd = new ProbeDesc();

		JrdsNode probeDescNode = n.getChild(CompiledXPath.get("/probedesc"));
		probeDescNode.setMethod(pd, CompiledXPath.get("probeName"), "setProbeName");
		probeDescNode.setMethod(pd, CompiledXPath.get("name"), "setName");

		logger.trace("Creating probe description " + pd.getName());

		String className = probeDescNode.evaluate(CompiledXPath.get("probeClass")).trim();
		Class<? extends Probe> c = (Class<? extends Probe>) classLoader.loadClass(className);
		pd.setProbeClass(c);

		if(probeDescNode.checkPath(CompiledXPath.get("uniq")))
			pd.setUniqIndex(true);

		String uptimefactorStr = "";
		try {
			uptimefactorStr = probeDescNode.evaluate(CompiledXPath.get("uptimefactor")).trim();
			if( uptimefactorStr != null && ! "".equals(uptimefactorStr)) {
				float uptimefactor = Float.parseFloat(uptimefactorStr);
				pd.setUptimefactor(uptimefactor);
			}
		} catch (NumberFormatException e) {
			logger.warn("Uptime factor not valid " + uptimefactorStr);
			pd.setUptimefactor(0);
		}

		pd.setGraphClasses(probeDescNode.doTreeList(CompiledXPath.get("graphs/name"), new JrdsNode.FilterNode() {
			@Override
			public Object filter(Node input) {
				logger.trace("Adding graph: " + input.getTextContent());
				return input.getTextContent();
			}

		}));

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
			for(Object o:  makeArgs(argsNode)) {
				pd.addDefaultArg(o);
			}

		for(JrdsNode dsNode: probeDescNode.iterate(CompiledXPath.get("ds"))) {
			Map<String, Object> dsMap = new HashMap<String, Object>(4);
			for(JrdsNode dsContent: dsNode.iterate(CompiledXPath.get("*"))) {
				String element = dsContent.getNodeName();
				String textValue = dsContent.getTextContent().trim();
				Object value = textValue;
				if("collect".equals(element))
					element ="collectKey";
				if("dsType".equals(element)) {
					if( !"NONE".equals(textValue.toUpperCase()))
						value = DsType.valueOf(textValue.toUpperCase());
					else
						value = null;
				}
				else if("oid".equals(element)) {
					value = new OID(textValue);
					element ="collectKey";
				}
				dsMap.put(element, value);
			}
			pd.add(dsMap);
		}
		
		Map<String, String> props = makeProperties(probeDescNode);
		if(props != null)
			pd.setProperties(props);
		
		return pd;
	}

	@Override
	public
	void setProperty(ObjectBuilder.properties name, Object o) {
		switch(name) {
		case CLASSLOADER:
			classLoader = (ClassLoader) o;
			break;
		default:
			super.setProperty(name, o);
		}
	}

}
