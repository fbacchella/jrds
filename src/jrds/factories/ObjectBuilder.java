package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.PropertiesManager;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;

public abstract class ObjectBuilder {
	static final private Logger logger = Logger.getLogger(ObjectBuilder.class);

	public enum properties {
		MACRO, GRAPHDESC, PROBEDESC, ARGFACTORY, PROBEFACTORY, GRAPHFACTORY, LOADER, CLASSLOADER, PM;
	}

	private ArgFactory af = new ArgFactory();
	PropertiesManager pm;

	abstract Object build(JrdsNode n) throws InvocationTargetException;

	public void setProperty(properties name, Object o) {
		switch(name) {
		case ARGFACTORY:
			af = (ArgFactory) o;
			break;
		case PM:
			pm = (PropertiesManager) o;
			break;
		}
	}

	public List<Object> makeArgs(JrdsNode n) {
		List<Object> argsList = new ArrayList<Object>(5);
		for(JrdsNode argNode: n.iterate(CompiledXPath.get("arg"))) {
			Map<String,String> argMap = argNode.attrMap();
			String type = argMap.get("type");
			String value = argMap.get("value");
			Object o = af.makeArg(type, value);
			argsList.add(o);
		}
		for(JrdsNode argNode: n.iterate(CompiledXPath.get("list"))) {
			argsList.add(makeArgs(argNode));
		}
		logger.trace("arg vector: " + argsList);
		return argsList;
	}

	public Object makeArg(String className, String value) {
		return af.makeArg(className, value);
	}
	
	public Map<String, String> makeProperties(JrdsNode n) {
		if(n == null)
			return null;
		NodeListIterator propsNodes = n.iterate(CompiledXPath.get("properties/entry"));
		if(propsNodes.getLength() == 0) {
			return null;
		}
		Map<String, String> props = new HashMap<String, String>();
		for(JrdsNode propNode: propsNodes) {
			String key = propNode.evaluate(CompiledXPath.get("@key"));
			String value = propNode.getTextContent();
			logger.trace("Adding propertie " + key + ": " + value);
			props.put(key, value);
		}
		return props;
	}


}
