package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jrds.PropertiesManager;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;

public abstract class ObjectBuilder {
	static final private Logger logger = Logger.getLogger(ObjectBuilder.class);

	public enum properties {
		MACRO, GRAPHDESC, PROBEDESC, PROBEFACTORY, GRAPHFACTORY, LOADER, CLASSLOADER, PM;
	}

	PropertiesManager pm;

	abstract Object build(JrdsNode n) throws InvocationTargetException;

	public void setProperty(properties name, Object o) {
		switch(name) {
		case PM:
			pm = (PropertiesManager) o;
			break;
		}
	}

	public Map<String, String> makeProperties(JrdsNode n) {
		if(n == null)
			return Collections.emptyMap();
		NodeListIterator propsNodes = n.iterate(CompiledXPath.get("properties/entry"));
		if(propsNodes.getLength() == 0) {
			return Collections.emptyMap();
		}
		Map<String, String> props = new HashMap<String, String>();
		for(JrdsNode propNode: propsNodes) {
			String key = propNode.evaluate(CompiledXPath.get("@key"));
			String value = propNode.getTextContent();
			logger.trace("Adding propertie " + key + "=" + value);
			props.put(key, value);
		}
		logger.debug("Properties map: " + props);
		return props;
	}


}
