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

		n.setMethod(pd, CompiledXPath.get("/probedesc/probeName"), "setProbeName");
		n.setMethod(pd, CompiledXPath.get("/probedesc/name"), "setName");

		logger.trace("Creating probe description " + pd.getName());

		String className = n.evaluate(CompiledXPath.get("/probedesc/probeClass")).trim();
		Class<? extends Probe> c = (Class<? extends Probe>) classLoader.loadClass(className);
		pd.setProbeClass(c);

		if(n.checkPath(CompiledXPath.get("/probedesc/uniq")))
			pd.setUniqIndex(true);

		String uptimefactorStr = "";
		try {
			uptimefactorStr = n.evaluate(CompiledXPath.get("/probedesc/uptimefactor")).trim();
			if( uptimefactorStr != null && ! "".equals(uptimefactorStr)) {
				float uptimefactor = Float.parseFloat(uptimefactorStr);
				pd.setUptimefactor(uptimefactor);
			}
		} catch (NumberFormatException e) {
			logger.warn("Uptime factor not valid " + uptimefactorStr);
			pd.setUptimefactor(0);
		}

		pd.setGraphClasses(n.doTreeList(CompiledXPath.get("/probedesc/graphs/name"), new JrdsNode.FilterNode() {
			@Override
			public Object filter(Node input) {
				logger.trace("Adding graph: " + input.getTextContent());
				return input.getTextContent();
			}

		}));

		for(JrdsNode specificNode: n.iterate(CompiledXPath.get("/probedesc/specific"))) {
			Map<String, String> m = specificNode.attrMap();
			if(m != null) {
				String name = m.get("name");
				String value = specificNode.getTextContent().trim();
				pd.addSpecific(name, value);
				logger.trace("Specific added: " + name + "='" + value + "'");
			}
		}

		String snmpRequester = n.evaluate(CompiledXPath.get("/probedesc/snmpRequester"));
		if(snmpRequester != null && ! "".equals(snmpRequester.trim())) {
			pd.addSpecific("requester", snmpRequester.trim());
			logger.trace("Specific added: requester='" + snmpRequester.trim() + "'");

		}

		String index = n.evaluate(CompiledXPath.get("/probedesc/index"));
		if(index !=null && ! "".equals(index))	{
			pd.addSpecific(jrds.probe.snmp.RdsIndexedSnmpRrd.INDEXOIDNAME, index.trim());
		}

		//Populating default argument vector
		JrdsNode argsNode = n.getChild(CompiledXPath.get("/probedesc/defaultargs"));
		if(argsNode != null)
			for(Object o:  makeArgs(argsNode)) {
				pd.addDefaultArg(o);
			}

		for(JrdsNode dsNode: n.iterate(CompiledXPath.get("/probedesc/ds"))) {
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
