package jrds.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.ArgFactory;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;

public abstract class ObjectBuilder {
	static final private Logger logger = Logger.getLogger(ObjectBuilder.class);

	public enum properties {
		MACRO, GRAPHDESC, PROBEDESC, ARGFACTORY, PROBEFACTORY, GRAPHFACTORY, LOADER, CLASSLOADER;
	}

	private ArgFactory af = new ArgFactory();

	abstract Object build(JrdsNode n);

	void setProperty(properties name, Object o) {
		switch(name) {
		case ARGFACTORY:
			af = (ArgFactory) o;
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

}