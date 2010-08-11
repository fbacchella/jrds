package jrds.factories;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpression;

import jrds.PropertiesManager;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;
import jrds.factories.xml.JrdsNode.FilterNode;
import jrds.webapp.RolesACL;
import jrds.webapp.WithACL;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;

public abstract class ObjectBuilder {
	static final private Logger logger = Logger.getLogger(ObjectBuilder.class);

	public enum properties {
		MACRO, GRAPHDESC, PROBEDESC, PROBEFACTORY, GRAPHFACTORY, LOADER, CLASSLOADER, PM, DEFAULTROLE;
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

	/**
	 * Add a roles ACL to the object being build, but only if security was set in the properties.
	 * If the xpath match no roles, the object will have no ACL set, so it will use it's own default ACL.
	 * 
	 * @param object The object to add a role to
	 * @param n  The DOM tree where the xpath will look into
	 * @param xpath where to found the roles
	 */
	protected void doACL(WithACL object, JrdsNode n, XPathExpression xpath) {
		if(pm.security){
			List<String> roles = n.doTreeList(xpath, new FilterNode<String>() {
				@Override
				public String filter(Node input) {
					return input.getTextContent();
				}
			}
			);
			if(roles.size() > 0) {				
				object.addACL(new RolesACL(new HashSet<String>(roles)));
				object.addACL(pm.adminACL);
			}
			else {
				object.addACL(pm.defaultACL);
			}
		}
	}

}
